package com.example.cep_backend.profile.dto;

public record OtherProfileOverviewDto(
        Long userId,
        String avatar,
        String username,
        String city,
        long fans,
        long following,
        String bio,
        String sellerCredit,
        String buyerCredit,
        boolean followed) {
}
