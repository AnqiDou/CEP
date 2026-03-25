package com.example.cep_backend.home.dto;

import java.util.List;

public record HomeItemListDto(List<HomeItemDto> items, long total, int page, int size) {
}
