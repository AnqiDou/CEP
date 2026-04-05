package com.example.cep_backend.profile.dto;

public record OtherProfileOverviewDto(
                Long userId,
                String avatar,
                String username,
                long fans,
                long following,
                String sellerCredit,
                String buyerCredit,
                boolean followed) {
}
