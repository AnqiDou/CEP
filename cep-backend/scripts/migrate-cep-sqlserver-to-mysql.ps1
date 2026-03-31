param(
    [string]$SqlServerHost = "LAPTOP-CCRK0TGK",
    [int]$SqlServerPort = 1433,
    [string]$SqlServerUser = "aq",
    [string]$SqlServerPassword = "5187153daq",
    [string]$SqlServerDatabase = "CEP",

    [string]$MySqlHost = "localhost",
    [int]$MySqlPort = 3306,
    [string]$MySqlUser = "root",
    [string]$MySqlPassword = "",
    [string]$MySqlDatabase = "CEP"
)

$ErrorActionPreference = "Stop"

function Escape-MySqlString([string]$s) {
    if ($null -eq $s) { return "" }
    return $s.Replace("\\", "\\\\").Replace("'", "''").Replace("`0", "")
}

function Quote-MySqlIdent([string]$name) {
    $bt = [char]96
    $safe = $name.Replace([string][char]96, "")
    return [string]::Concat($bt, $safe, $bt)
}

function Normalize-SqlDefault([string]$defaultExpr) {
    if ([string]::IsNullOrWhiteSpace($defaultExpr)) { return $null }

    $v = $defaultExpr.Trim()
    while ($v.StartsWith("(") -and $v.EndsWith(")")) {
        $v = $v.Substring(1, $v.Length - 2).Trim()
    }

    $low = $v.ToLowerInvariant()
    if ($low -in @("getdate", "sysdatetime", "sysutcdatetime", "current_timestamp")) {
        return "CURRENT_TIMESTAMP"
    }

    if ($v -match "^n?'(.*)'$") {
        $content = $Matches[1]
        return "'" + (Escape-MySqlString $content) + "'"
    }

    if ($v -match "^[+-]?\d+(\.\d+)?$") {
        return $v
    }

    return $null
}

function Convert-SqlTypeToMySql($sqlType, $charMaxLen, $numPrecision, $numScale, $isIdentity) {
    $t = $sqlType.ToLowerInvariant()

    switch ($t) {
        "bigint" { return "BIGINT" + ($(if ($isIdentity) { " AUTO_INCREMENT" } else { "" })) }
        "int" { return "INT" + ($(if ($isIdentity) { " AUTO_INCREMENT" } else { "" })) }
        "smallint" { return "SMALLINT" }
        "tinyint" { return "TINYINT" }
        "bit" { return "TINYINT(1)" }
        "decimal" { return "DECIMAL($numPrecision,$numScale)" }
        "numeric" { return "DECIMAL($numPrecision,$numScale)" }
        "money" { return "DECIMAL(19,4)" }
        "smallmoney" { return "DECIMAL(10,4)" }
        "float" { return "DOUBLE" }
        "real" { return "FLOAT" }
        "char" {
            if ($charMaxLen -gt 0) { return "CHAR($charMaxLen)" }
            return "CHAR(1)"
        }
        "nchar" {
            if ($charMaxLen -gt 0) { return "CHAR($charMaxLen)" }
            return "CHAR(1)"
        }
        "varchar" {
            if ($charMaxLen -eq -1) { return "LONGTEXT" }
            return "VARCHAR($charMaxLen)"
        }
        "nvarchar" {
            if ($charMaxLen -eq -1) { return "LONGTEXT" }
            return "VARCHAR($charMaxLen)"
        }
        "text" { return "LONGTEXT" }
        "ntext" { return "LONGTEXT" }
        "date" { return "DATE" }
        "time" { return "TIME(6)" }
        "datetime" { return "DATETIME" }
        "smalldatetime" { return "DATETIME" }
        "datetime2" { return "DATETIME(6)" }
        "datetimeoffset" { return "VARCHAR(40)" }
        "uniqueidentifier" { return "CHAR(36)" }
        "binary" {
            if ($charMaxLen -gt 0) { return "BINARY($charMaxLen)" }
            return "BLOB"
        }
        "varbinary" {
            if ($charMaxLen -eq -1) { return "LONGBLOB" }
            return "VARBINARY($charMaxLen)"
        }
        "image" { return "LONGBLOB" }
        default { return "LONGTEXT" }
    }
}

function Convert-ValueToMySqlLiteral($v) {
    if ($null -eq $v -or $v -is [System.DBNull]) { return "NULL" }

    if ($v -is [bool]) {
        if ($v) { return "1" }
        return "0"
    }

    if ($v -is [byte[]]) {
        if ($v.Length -eq 0) { return "x''" }
        $hex = [System.BitConverter]::ToString($v).Replace("-", "")
        return "x'$hex'"
    }

    if ($v -is [DateTime]) {
        return "'" + $v.ToString("yyyy-MM-dd HH:mm:ss.ffffff") + "'"
    }

    if ($v -is [DateTimeOffset]) {
        return "'" + (Escape-MySqlString $v.ToString("o")) + "'"
    }

    if ($v -is [Guid]) {
        return "'" + $v.ToString() + "'"
    }

    if ($v -is [string]) {
        return "'" + (Escape-MySqlString $v) + "'"
    }

    if ($v -is [System.IFormattable]) {
        return $v.ToString($null, [System.Globalization.CultureInfo]::InvariantCulture)
    }

    return "'" + (Escape-MySqlString ([string]$v)) + "'"
}

$connStr = "Server=$SqlServerHost,$SqlServerPort;Database=$SqlServerDatabase;User ID=$SqlServerUser;Password=$SqlServerPassword;TrustServerCertificate=True;Encrypt=True;"
$conn = New-Object System.Data.SqlClient.SqlConnection($connStr)
$conn.Open()

try {
    $scriptDir = Join-Path (Get-Location) "cep-backend\scripts"
    if (-not (Test-Path $scriptDir)) {
        New-Item -Path $scriptDir -ItemType Directory | Out-Null
    }

    $outFile = Join-Path $scriptDir "cep_sqlserver_to_mysql.sql"
    $sw = New-Object System.IO.StreamWriter($outFile, $false, (New-Object System.Text.UTF8Encoding($false)))

    try {
        $sw.WriteLine("-- Auto generated: SQL Server => MySQL")
        $sw.WriteLine("SET NAMES utf8mb4;")
        $sw.WriteLine("SET FOREIGN_KEY_CHECKS = 0;")
        $sw.WriteLine("DROP DATABASE IF EXISTS $(Quote-MySqlIdent $MySqlDatabase);")
        $sw.WriteLine("CREATE DATABASE $(Quote-MySqlIdent $MySqlDatabase) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
        $sw.WriteLine("USE $(Quote-MySqlIdent $MySqlDatabase);")
        $sw.WriteLine("")

        # 获取表清单
        $tableCmd = $conn.CreateCommand()
        $tableCmd.CommandText = @"
SELECT TABLE_SCHEMA, TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE'
  AND TABLE_CATALOG = DB_NAME()
ORDER BY TABLE_SCHEMA, TABLE_NAME;
"@
        $tables = New-Object System.Collections.Generic.List[object]
        $r = $tableCmd.ExecuteReader()
        while ($r.Read()) {
            $tables.Add([PSCustomObject]@{
                Schema = $r.GetString(0)
                Name   = $r.GetString(1)
            }) | Out-Null
        }
        $r.Close()

        $fkStatements = New-Object System.Collections.Generic.List[string]

        foreach ($t in $tables) {
            $schema = $t.Schema
            $table = $t.Name
            $sw.WriteLine("DROP TABLE IF EXISTS $(Quote-MySqlIdent $table);")

            # 列信息
            $colCmd = $conn.CreateCommand()
            $colCmd.CommandText = @"
SELECT c.COLUMN_NAME,
       c.DATA_TYPE,
       c.CHARACTER_MAXIMUM_LENGTH,
       c.NUMERIC_PRECISION,
       c.NUMERIC_SCALE,
       c.IS_NULLABLE,
       c.COLUMN_DEFAULT,
       COLUMNPROPERTY(object_id(c.TABLE_SCHEMA + '.' + c.TABLE_NAME), c.COLUMN_NAME, 'IsIdentity') AS IS_IDENTITY,
       c.ORDINAL_POSITION
FROM INFORMATION_SCHEMA.COLUMNS c
WHERE c.TABLE_SCHEMA = @schema AND c.TABLE_NAME = @table
ORDER BY c.ORDINAL_POSITION;
"@
            $null = $colCmd.Parameters.Add("@schema", [System.Data.SqlDbType]::NVarChar, 128)
            $colCmd.Parameters["@schema"].Value = $schema
            $null = $colCmd.Parameters.Add("@table", [System.Data.SqlDbType]::NVarChar, 128)
            $colCmd.Parameters["@table"].Value = $table

            $cols = New-Object System.Collections.Generic.List[object]
            $cr = $colCmd.ExecuteReader()
            while ($cr.Read()) {
                $cols.Add([PSCustomObject]@{
                    Name = [string]$cr[0]
                    DataType = [string]$cr[1]
                    CharMaxLen = $(if ($cr.IsDBNull(2)) { $null } else { [int]$cr[2] })
                    NumPrecision = $(if ($cr.IsDBNull(3)) { $null } else { [int]$cr[3] })
                    NumScale = $(if ($cr.IsDBNull(4)) { $null } else { [int]$cr[4] })
                    IsNullable = ([string]$cr[5] -eq "YES")
                    DefaultExpr = $(if ($cr.IsDBNull(6)) { $null } else { [string]$cr[6] })
                    IsIdentity = $(if ($cr.IsDBNull(7)) { $false } else { [int]$cr[7] -eq 1 })
                    Ordinal = [int]$cr[8]
                }) | Out-Null
            }
            $cr.Close()

            # 主键
            $pkCmd = $conn.CreateCommand()
            $pkCmd.CommandText = @"
SELECT ku.COLUMN_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku
  ON tc.CONSTRAINT_NAME = ku.CONSTRAINT_NAME
 AND tc.TABLE_SCHEMA = ku.TABLE_SCHEMA
 AND tc.TABLE_NAME = ku.TABLE_NAME
WHERE tc.TABLE_SCHEMA = @schema
  AND tc.TABLE_NAME = @table
  AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
ORDER BY ku.ORDINAL_POSITION;
"@
            $null = $pkCmd.Parameters.Add("@schema", [System.Data.SqlDbType]::NVarChar, 128)
            $pkCmd.Parameters["@schema"].Value = $schema
            $null = $pkCmd.Parameters.Add("@table", [System.Data.SqlDbType]::NVarChar, 128)
            $pkCmd.Parameters["@table"].Value = $table

            $pkCols = New-Object System.Collections.Generic.List[string]
            $pr = $pkCmd.ExecuteReader()
            while ($pr.Read()) {
                $pkCols.Add([string]$pr[0]) | Out-Null
            }
            $pr.Close()

            # 唯一约束（不含主键）
            $uqCmd = $conn.CreateCommand()
            $uqCmd.CommandText = @"
SELECT tc.CONSTRAINT_NAME, ku.COLUMN_NAME, ku.ORDINAL_POSITION
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE ku
  ON tc.CONSTRAINT_NAME = ku.CONSTRAINT_NAME
 AND tc.TABLE_SCHEMA = ku.TABLE_SCHEMA
 AND tc.TABLE_NAME = ku.TABLE_NAME
WHERE tc.TABLE_SCHEMA = @schema
  AND tc.TABLE_NAME = @table
  AND tc.CONSTRAINT_TYPE = 'UNIQUE'
ORDER BY tc.CONSTRAINT_NAME, ku.ORDINAL_POSITION;
"@
            $null = $uqCmd.Parameters.Add("@schema", [System.Data.SqlDbType]::NVarChar, 128)
            $uqCmd.Parameters["@schema"].Value = $schema
            $null = $uqCmd.Parameters.Add("@table", [System.Data.SqlDbType]::NVarChar, 128)
            $uqCmd.Parameters["@table"].Value = $table

            $uqMap = @{}
            $ur = $uqCmd.ExecuteReader()
            while ($ur.Read()) {
                $cname = [string]$ur[0]
                $col = [string]$ur[1]
                if (-not $uqMap.ContainsKey($cname)) {
                    $uqMap[$cname] = New-Object System.Collections.Generic.List[string]
                }
                $uqMap[$cname].Add($col) | Out-Null
            }
            $ur.Close()

            # 外键
            $fkCmd = $conn.CreateCommand()
            $fkCmd.CommandText = @"
SELECT fk.name AS fk_name,
       c1.name AS parent_col,
       OBJECT_SCHEMA_NAME(fk.referenced_object_id) AS ref_schema,
       OBJECT_NAME(fk.referenced_object_id) AS ref_table,
       c2.name AS ref_col,
       fkc.constraint_column_id
FROM sys.foreign_keys fk
JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
JOIN sys.columns c1 ON c1.object_id = fkc.parent_object_id AND c1.column_id = fkc.parent_column_id
JOIN sys.columns c2 ON c2.object_id = fkc.referenced_object_id AND c2.column_id = fkc.referenced_column_id
WHERE OBJECT_SCHEMA_NAME(fk.parent_object_id) = @schema
  AND OBJECT_NAME(fk.parent_object_id) = @table
ORDER BY fk.name, fkc.constraint_column_id;
"@
            $null = $fkCmd.Parameters.Add("@schema", [System.Data.SqlDbType]::NVarChar, 128)
            $fkCmd.Parameters["@schema"].Value = $schema
            $null = $fkCmd.Parameters.Add("@table", [System.Data.SqlDbType]::NVarChar, 128)
            $fkCmd.Parameters["@table"].Value = $table

            $fkMap = @{}
            $fr = $fkCmd.ExecuteReader()
            while ($fr.Read()) {
                $fkName = [string]$fr[0]
                $parentCol = [string]$fr[1]
                $refTable = [string]$fr[3]
                $refCol = [string]$fr[4]

                if (-not $fkMap.ContainsKey($fkName)) {
                    $fkMap[$fkName] = [PSCustomObject]@{
                        RefTable = $refTable
                        ParentCols = New-Object System.Collections.Generic.List[string]
                        RefCols = New-Object System.Collections.Generic.List[string]
                    }
                }
                $fkMap[$fkName].ParentCols.Add($parentCol) | Out-Null
                $fkMap[$fkName].RefCols.Add($refCol) | Out-Null
            }
            $fr.Close()

            $lineList = New-Object System.Collections.Generic.List[string]
            foreach ($c in $cols) {
                $numPrecision = $(if ($null -eq $c.NumPrecision) { 18 } else { $c.NumPrecision })
                $numScale = $(if ($null -eq $c.NumScale) { 0 } else { $c.NumScale })
                $charLen = $(if ($null -eq $c.CharMaxLen) { 255 } else { $c.CharMaxLen })

                # SQL Server nvarchar/nchar 的长度是字符数（通常已正确），直接使用
                $mappedType = Convert-SqlTypeToMySql $c.DataType $charLen $numPrecision $numScale $c.IsIdentity
                $nullable = $(if ($c.IsNullable) { "NULL" } else { "NOT NULL" })
                $defaultPart = ""
                if (-not $c.IsIdentity) {
                    $d = Normalize-SqlDefault $c.DefaultExpr
                    if ($null -ne $d) {
                        $defaultPart = " DEFAULT $d"
                    }
                }
                $lineList.Add("  $(Quote-MySqlIdent $c.Name) $mappedType $nullable$defaultPart") | Out-Null
            }

            if ($pkCols.Count -gt 0) {
                $pkText = ($pkCols | ForEach-Object { Quote-MySqlIdent $_ }) -join ", "
                $lineList.Add("  PRIMARY KEY ($pkText)") | Out-Null
            }

            foreach ($k in $uqMap.Keys) {
                $colsText = ($uqMap[$k] | ForEach-Object { Quote-MySqlIdent $_ }) -join ", "
                $safeName = $k.Replace([string][char]96, "")
                $lineList.Add("  UNIQUE KEY $(Quote-MySqlIdent $safeName) ($colsText)") | Out-Null
            }

            $sw.WriteLine("CREATE TABLE $(Quote-MySqlIdent $table) (")
            $sw.WriteLine(($lineList -join ",`r`n"))
            $sw.WriteLine(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;")
            $sw.WriteLine("")

            foreach ($fkName in $fkMap.Keys) {
                $pCols = ($fkMap[$fkName].ParentCols | ForEach-Object { Quote-MySqlIdent $_ }) -join ", "
                $rCols = ($fkMap[$fkName].RefCols | ForEach-Object { Quote-MySqlIdent $_ }) -join ", "
                $refTable = $fkMap[$fkName].RefTable
                $safeFk = $fkName.Replace([string][char]96, "")
                $fkStatements.Add("ALTER TABLE $(Quote-MySqlIdent $table) ADD CONSTRAINT $(Quote-MySqlIdent $safeFk) FOREIGN KEY ($pCols) REFERENCES $(Quote-MySqlIdent $refTable) ($rCols);") | Out-Null
            }
        }

        # 导出数据
        foreach ($t in $tables) {
            $schema = $t.Schema
            $table = $t.Name

            $dataCmd = $conn.CreateCommand()
            $dataCmd.CommandTimeout = 0
            $dataCmd.CommandText = "SELECT * FROM [$schema].[$table];"
            $dr = $dataCmd.ExecuteReader()

            $fieldCount = $dr.FieldCount
            $colNames = New-Object System.Collections.Generic.List[string]
            for ($i = 0; $i -lt $fieldCount; $i++) {
                $colNames.Add((Quote-MySqlIdent $dr.GetName($i))) | Out-Null
            }
            $colText = $colNames -join ", "

            $batch = New-Object System.Collections.Generic.List[string]
            $batchSize = 500
            $rowCount = 0

            while ($dr.Read()) {
                $vals = New-Object System.Collections.Generic.List[string]
                for ($i = 0; $i -lt $fieldCount; $i++) {
                    $vals.Add((Convert-ValueToMySqlLiteral $dr.GetValue($i))) | Out-Null
                }
                $batch.Add("(" + ($vals -join ", ") + ")") | Out-Null
                $rowCount++

                if ($batch.Count -ge $batchSize) {
                    $sw.WriteLine("INSERT INTO $(Quote-MySqlIdent $table) ($colText) VALUES")
                    $sw.WriteLine(($batch -join ",`r`n") + ";")
                    $batch.Clear()
                }
            }

            if ($batch.Count -gt 0) {
                $sw.WriteLine("INSERT INTO $(Quote-MySqlIdent $table) ($colText) VALUES")
                $sw.WriteLine(($batch -join ",`r`n") + ";")
            }

            $dr.Close()
            $sw.WriteLine("")
            Write-Host "Exported table $table : $rowCount rows"
        }

        # 最后添加外键
        foreach ($fk in $fkStatements) {
            $sw.WriteLine($fk)
        }

        $sw.WriteLine("SET FOREIGN_KEY_CHECKS = 1;")
    }
    finally {
        $sw.Dispose()
    }

    $mysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    if (-not (Test-Path $mysqlExe)) {
        throw "mysql.exe not found: $mysqlExe"
    }

    $cmd = "`"$mysqlExe`" -h $MySqlHost -P $MySqlPort -u $MySqlUser -p$MySqlPassword < `"$outFile`""
    Write-Host "Executing MySQL import..."
    cmd /c $cmd
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL import failed with exit code $LASTEXITCODE"
    }

    Write-Host "Done. SQL file: $outFile"
}
finally {
    $conn.Close()
    $conn.Dispose()
}

