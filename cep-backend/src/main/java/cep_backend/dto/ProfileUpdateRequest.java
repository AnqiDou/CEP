package cep_backend.dto;

public record ProfileUpdateRequest(
                String username,
                String name,
                String phone,
                String address,
                String avatar) {
}
