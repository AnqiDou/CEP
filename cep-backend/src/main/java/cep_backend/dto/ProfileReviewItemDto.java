package cep_backend.dto;

public record ProfileReviewItemDto(
                Long id,
                String user,
                String avatar,
                String identity,
                String rating,
                String content,
                String time) {
}
