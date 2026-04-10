$ErrorActionPreference = 'Stop'

function Ensure-Dir([string]$Path) {
  if (!(Test-Path $Path)) {
    New-Item -ItemType Directory -Path $Path -Force | Out-Null
  }
}

function Normalize-Rel([string]$Path) {
  return ($Path -replace '\\', '/')
}

function Resolve-TargetDir([string]$NewRoot, [string]$Rel) {
  if ($Rel -eq 'CepBackendApplication.java') { return $NewRoot }
  if ($Rel -like 'config/*') { return (Join-Path $NewRoot 'config') }
  if ($Rel -like 'common/api/*') { return (Join-Path $NewRoot 'common/result') }
  if ($Rel -like 'common/exception/*') { return (Join-Path $NewRoot 'common/exception') }
  if ($Rel -eq 'auth/BusinessException.java' -or $Rel -eq 'auth/UnauthorizedException.java') { return (Join-Path $NewRoot 'common/exception') }
  if ($Rel -like 'message/ws/*') { return (Join-Path $NewRoot 'util/ws') }
  if ($Rel -match '/controller/') { return (Join-Path $NewRoot 'controller') }
  if ($Rel -match '/dto/') { return (Join-Path $NewRoot 'dto') }
  if ($Rel -match '/repository/') { return (Join-Path $NewRoot 'mapper') }
  if ($Rel -match '/model/') { return (Join-Path $NewRoot 'entity/po') }
  if ($Rel -match '/service/') { return (Join-Path $NewRoot 'service') }
  return $NewRoot
}

function Map-Import([string]$ImportFqn) {
  $v = $ImportFqn
  $v = $v -replace '^com\.example\.cep_backend', 'cep_backend'

  if ($v -notmatch '^cep_backend\.') { return $v }

  $v = $v -replace '^cep_backend\.common\.api\.', 'cep_backend.common.result.'
  $v = $v -replace '^cep_backend\.auth\.BusinessException$', 'cep_backend.common.exception.BusinessException'
  $v = $v -replace '^cep_backend\.auth\.UnauthorizedException$', 'cep_backend.common.exception.UnauthorizedException'

  $v = $v -replace '^cep_backend\.(?:[^.]+\.)*controller\.', 'cep_backend.controller.'
  $v = $v -replace '^cep_backend\.(?:[^.]+\.)*dto\.', 'cep_backend.dto.'
  $v = $v -replace '^cep_backend\.(?:[^.]+\.)*repository\.', 'cep_backend.mapper.'
  $v = $v -replace '^cep_backend\.(?:[^.]+\.)*model\.', 'cep_backend.entity.po.'
  $v = $v -replace '^cep_backend\.(?:[^.]+\.)*service\.', 'cep_backend.service.'
  $v = $v -replace '^cep_backend\.message\.ws\.', 'cep_backend.util.ws.'
  return $v
}

$mainBase = 'cep-backend/src/main/java'
$oldMain = Join-Path $mainBase 'com/example/cep_backend'
$newMain = Join-Path $mainBase 'cep_backend'

$testBase = 'cep-backend/src/test/java'
$oldTest = Join-Path $testBase 'com/example/cep_backend'
$newTest = Join-Path $testBase 'cep_backend'

@(
  $newMain,
  (Join-Path $newMain 'config'),
  (Join-Path $newMain 'controller'),
  (Join-Path $newMain 'service'),
  (Join-Path $newMain 'mapper'),
  (Join-Path $newMain 'entity/po'),
  (Join-Path $newMain 'dto'),
  (Join-Path $newMain 'common/result'),
  (Join-Path $newMain 'common/exception'),
  (Join-Path $newMain 'util/ws'),
  (Join-Path $newMain 'enums'),
  $newTest
) | ForEach-Object { Ensure-Dir $_ }

if (Test-Path $oldMain) {
  $oldMainAbs = (Resolve-Path $oldMain).Path
  Get-ChildItem -Path $oldMain -Recurse -File -Filter *.java | ForEach-Object {
    $relRaw = $_.FullName.Substring($oldMainAbs.Length + 1)
    $rel = Normalize-Rel $relRaw
    $destDir = Resolve-TargetDir -NewRoot $newMain -Rel $rel
    Ensure-Dir $destDir
    Move-Item -Path $_.FullName -Destination (Join-Path $destDir $_.Name) -Force
  }
}

if (Test-Path $oldTest) {
  $oldTestAbs = (Resolve-Path $oldTest).Path
  Get-ChildItem -Path $oldTest -Recurse -File -Filter *.java | ForEach-Object {
    $relRaw = $_.FullName.Substring($oldTestAbs.Length + 1)
    $rel = Normalize-Rel $relRaw
    $destDir = Join-Path $newTest (Split-Path $rel -Parent)
    Ensure-Dir $destDir
    Move-Item -Path $_.FullName -Destination (Join-Path $destDir $_.Name) -Force
  }
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Rewrite-Java([string]$FilePath, [string]$BasePath) {
  $text = [System.IO.File]::ReadAllText($FilePath).TrimStart([char]0xFEFF)

  $rel = Normalize-Rel ($FilePath.Substring((Resolve-Path $BasePath).Path.Length + 1))
  $relDir = Split-Path $rel -Parent
  $pkg = if ([string]::IsNullOrWhiteSpace($relDir)) { 'cep_backend' } else { $relDir.Replace('/', '.') }

  if ($text -match '(?m)^\s*package\s+[^;]+;') {
    $text = [regex]::Replace($text, '(?m)^\s*package\s+[^;]+;', "package $pkg;", 1)
  } else {
    $text = "package $pkg;`r`n`r`n" + $text
  }

  $text = [regex]::Replace($text, '(?m)^\s*import\s+([^;]+);', {
    param($m)
    $mapped = Map-Import $m.Groups[1].Value.Trim()
    "import $mapped;"
  })

  $text = $text -replace 'com\.example\.cep_backend', 'cep_backend'
  [System.IO.File]::WriteAllText($FilePath, $text, $utf8NoBom)
}

if (Test-Path $newMain) {
  Get-ChildItem -Path $newMain -Recurse -File -Filter *.java | ForEach-Object { Rewrite-Java $_.FullName $mainBase }
}
if (Test-Path $newTest) {
  Get-ChildItem -Path $newTest -Recurse -File -Filter *.java | ForEach-Object { Rewrite-Java $_.FullName $testBase }
}

if (Test-Path 'cep-backend/src/main/java/com') { Remove-Item -Path 'cep-backend/src/main/java/com' -Recurse -Force }
if (Test-Path 'cep-backend/src/test/java/com') { Remove-Item -Path 'cep-backend/src/test/java/com' -Recurse -Force }

Write-Output 'restructure-stable-done'
