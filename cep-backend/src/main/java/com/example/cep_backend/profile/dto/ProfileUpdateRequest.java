package com.example.cep_backend.profile.dto;

public record ProfileUpdateRequest(
                String username,
                String name,
                String phone,
                String address,
                String avatar) {
}
