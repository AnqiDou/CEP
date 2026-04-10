package cep_backend.dto;

public record ItemReportCreateResultDto(
        Long conversationId,
        String status,
        String message) {
}
