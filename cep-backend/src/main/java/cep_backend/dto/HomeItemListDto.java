package cep_backend.dto;
import java.util.List;

public record HomeItemListDto(List<HomeItemDto> items, long total, int page, int size) {
}
