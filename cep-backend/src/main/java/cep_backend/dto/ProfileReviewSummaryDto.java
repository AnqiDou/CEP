package cep_backend.dto;
import java.util.List;

public record ProfileReviewSummaryDto(
        int total,
        int goodCount,
        int badCount,
        List<ProfileReviewItemDto> reviews) {
}
