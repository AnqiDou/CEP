package cep_backend.dto;
import java.util.List;

public record OtherProfileReviewSummaryDto(
        int total,
        int goodCount,
        int badCount,
        List<OtherProfileReviewItemDto> reviews) {
}
