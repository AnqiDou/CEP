package com.example.cep_backend.review.dto;

public record SubmitReviewRequest(
        String rating,
        String content) {
}
