package cep_backend.dto;
import java.util.List;

public record HomeCategoryDto(Long id, String code, String name, String description, List<String> tags) {
}
