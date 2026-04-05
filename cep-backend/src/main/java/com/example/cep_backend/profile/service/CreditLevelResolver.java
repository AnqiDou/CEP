package com.example.cep_backend.profile.service;

public final class CreditLevelResolver {
    private CreditLevelResolver() {
    }

    public static String resolveLabel(int goodCount, int badCount) {
        int score = 100 + goodCount - badCount;
        if (score < 90) {
            return "较差";
        }
        if (score < 110) {
            return "良好";
        }
        if (score < 140) {
            return "优秀";
        }
        return "极好";
    }
}
