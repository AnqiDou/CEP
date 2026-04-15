package cep_backend.dto;

import java.time.LocalDateTime;

public record AdminUserCreditReviewDto(
        Long id,
        Long orderId,
        String rater,
        String target,
        String targetRole,
        String rating,
        String content,
        LocalDateTime createdAt) {
}

