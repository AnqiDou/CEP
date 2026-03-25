package com.example.cep_backend.home.dto;

import java.util.List;

public record HomeCategoryDto(Long id, String code, String name, String description, List<String> tags) {
}
