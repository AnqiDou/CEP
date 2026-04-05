package com.example.cep_backend.profile.dto;

public record OtherProfileReviewItemDto(
                Long id,
                String user,
                String avatar,
                String identity,
                String rating,
                String content,
                String time,
                String tag) {
}
