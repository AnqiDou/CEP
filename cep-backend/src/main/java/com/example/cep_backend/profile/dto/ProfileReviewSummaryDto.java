package com.example.cep_backend.profile.dto;

import java.util.List;

public record ProfileReviewSummaryDto(
        int total,
        int goodCount,
        int badCount,
        List<ProfileReviewItemDto> reviews) {
}
