package cep_backend.dto;

public record SubmitReviewRequest(
        String rating,
        String content) {
}
