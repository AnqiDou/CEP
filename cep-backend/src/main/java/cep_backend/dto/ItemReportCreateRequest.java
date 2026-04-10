package cep_backend.dto;

public record ItemReportCreateRequest(
        String reportType,
        String content) {
}
