package cep_backend.dto;

public record ReviewOrderDetailDto(
        Long orderId,
        Long itemId,
        String itemTitle,
        String itemCover,
        Long targetUserId,
        String targetUserName,
        String targetRole,
        String status,
        boolean canSubmit) {
}
