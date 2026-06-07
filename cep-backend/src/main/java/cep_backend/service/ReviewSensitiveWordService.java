package cep_backend.service;

import cep_backend.common.exception.BusinessException;
import cep_backend.mapper.ReviewSensitiveWordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ReviewSensitiveWordService {

    private static final String BLOCKED_MESSAGE = "内容包含不当用语";

    private final ReviewSensitiveWordRepository reviewSensitiveWordRepository;

    public ReviewSensitiveWordService(ReviewSensitiveWordRepository reviewSensitiveWordRepository) {
        this.reviewSensitiveWordRepository = reviewSensitiveWordRepository;
    }

    public void validateReviewContent(String content) {
        validateContent(content, BLOCKED_MESSAGE);
    }

    public void validatePublishContent(String itemName, String description) {
        String merged = (itemName == null ? "" : itemName) + "\n" + (description == null ? "" : description);
        validateContent(merged, "物品名称或描述中包含违规词");
    }

    private void validateContent(String content, String blockedMessage) {
        if (content == null || content.isBlank()) {
            return;
        }

        String normalized = normalize(content);
        List<String> blockedWords = reviewSensitiveWordRepository.findEnabledWords();
        for (String blockedWord : blockedWords) {
            if (normalized.contains(normalize(blockedWord))) {
                throw new BusinessException(blockedMessage);
            }
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
