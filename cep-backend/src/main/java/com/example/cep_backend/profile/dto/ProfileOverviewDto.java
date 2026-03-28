package com.example.cep_backend.profile.dto;

public record ProfileOverviewDto(
        String avatar,
        String username,
        long fans,
        long following,
        String sellerCredit,
        String buyerCredit) {
}
