package cep_backend.service;

import cep_backend.common.exception.BusinessException;
import cep_backend.dto.ItemDetailDto;
import cep_backend.dto.ItemDetailPublisherDto;
import cep_backend.dto.ItemFavoriteStatusDto;
import cep_backend.dto.ItemReportCreateResultDto;
import cep_backend.entity.po.ItemDetailRecord;
import cep_backend.mapper.ItemDetailRepository;
import cep_backend.service.MessageNotificationService;
import cep_backend.service.CreditLevelResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ItemDetailService {
    private static final String DEFAULT_USAGE_DURATION = "未填写";
    private static final String DEFAULT_PUBLISHER_NAME = "校园用户";

    private final ItemDetailRepository itemDetailRepository;
    private final MessageNotificationService messageNotificationService;

    public ItemDetailService(
            ItemDetailRepository itemDetailRepository,
            MessageNotificationService messageNotificationService) {
        this.itemDetailRepository = itemDetailRepository;
        this.messageNotificationService = messageNotificationService;
    }

    public ItemDetailDto getItemDetail(Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException("物品 ID 无效");
        }

        ItemDetailRecord record = itemDetailRepository.findByItemId(itemId);
        if (record == null) {
            throw new BusinessException("物品不存在或已下架");
        }

        itemDetailRepository.increaseViewCount(itemId);
        List<String> photos = itemDetailRepository.findPhotoUrls(itemId)
                .stream()
                .filter(photoUrl -> photoUrl != null && !photoUrl.isBlank())
                .toList();

        ItemDetailPublisherDto publisher = new ItemDetailPublisherDto(
                record.publisherId(),
                normalizeText(record.publisherName(), DEFAULT_PUBLISHER_NAME),
                normalizeText(record.publisherAvatar(), ""),
                resolvePublisherCreditLevel(record.publisherGoodCount(), record.publisherBadCount()));

        return new ItemDetailDto(
                record.itemId(),
                record.categoryId(),
                record.categoryCode(),
                record.categoryName(),
                record.title(),
                record.price(),
                record.purchaseDate(),
                normalizeText(record.usageDuration(), DEFAULT_USAGE_DURATION),
                record.createdAt(),
                record.description(),
                photos,
                publisher);
    }

    public ItemFavoriteStatusDto getFavoriteStatus(Long userId, Long itemId) {
        validateItemId(itemId);
        boolean favorite = itemDetailRepository.isFavorite(userId, itemId);
        return new ItemFavoriteStatusDto(favorite);
    }

    @Transactional
    public ItemFavoriteStatusDto addFavorite(Long userId, Long itemId) {
        validateItemId(itemId);
        Long ownerUserId = itemDetailRepository.findItemOwnerUserId(itemId);
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new BusinessException("物品不存在");
        }
        if (ownerUserId.equals(userId)) {
            throw new BusinessException("不能收藏自己的物品");
        }

        boolean inserted = itemDetailRepository.addFavorite(userId, itemId, LocalDateTime.now());
        if (inserted) {
            messageNotificationService.notifyItemFavorited(itemId, ownerUserId, userId);
        }
        return new ItemFavoriteStatusDto(true);
    }

    @Transactional
    public ItemFavoriteStatusDto removeFavorite(Long userId, Long itemId) {
        validateItemId(itemId);
        itemDetailRepository.removeFavorite(userId, itemId, LocalDateTime.now());
        return new ItemFavoriteStatusDto(false);
    }

    @Transactional
    public ItemReportCreateResultDto createReport(Long reporterUserId, Long itemId, String reportType, String content) {
        validateItemId(itemId);
        if (reporterUserId == null || reporterUserId <= 0) {
            throw new BusinessException("用户参数无效");
        }
        ItemDetailRepository.ItemReportMeta meta = itemDetailRepository.findItemReportMetaByItemId(itemId);
        if (meta == null) {
            throw new BusinessException("物品不存在");
        }
        if (meta.publisherUserId() != null && meta.publisherUserId().equals(reporterUserId)) {
            throw new BusinessException("不能举报自己的物品");
        }
        if (itemDetailRepository.existsOpenReportForItemAndReporter(itemId, reporterUserId)) {
            throw new BusinessException("你已提交过该商品的处理中举报");
        }

        String normalizedType = normalizeReportType(reportType);
        String normalizedContent = normalizeReportContent(content);
        String title = buildConversationTitle(normalizedType);
        String preview = normalizedContent.length() > 100
                ? normalizedContent.substring(0, 100)
                : normalizedContent;
        LocalDateTime now = LocalDateTime.now();
        Long conversationId = itemDetailRepository.createAdminSupportConversation(
                title,
                normalizedType,
                reporterUserId,
                itemId,
                normalizedContent,
                preview,
                "OPEN",
                now);
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException("提交失败，请稍后重试");
        }
        itemDetailRepository.insertAdminSupportMessage(conversationId, "USER", normalizedContent, now);
        return new ItemReportCreateResultDto(conversationId, "OPEN", "举报已提交，管理员将尽快处理");
    }

    private void validateItemId(Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException("物品 ID 无效");
        }
    }

    private String resolvePublisherCreditLevel(Integer goodCount, Integer badCount) {
        int good = goodCount == null ? 0 : goodCount;
        int bad = badCount == null ? 0 : badCount;
        return "卖家信用" + CreditLevelResolver.resolveLabel(good, bad);
    }

    private String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String normalizeReportType(String reportType) {
        if (reportType == null || reportType.trim().isEmpty()) {
            return "PROHIBITED_CONTACT";
        }
        String value = reportType.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "PROHIBITED_CONTACT", "COUNTERFEIT", "WRONG_CATEGORY", "FRAUD_RISK", "OTHER" -> value;
            default -> throw new BusinessException("举报类型不支持");
        };
    }

    private String normalizeReportContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("举报内容不能为空");
        }
        String trimmed = content.trim();
        if (trimmed.length() < 5) {
            throw new BusinessException("举报内容至少5个字");
        }
        if (trimmed.length() > 500) {
            throw new BusinessException("举报内容不能超过500字");
        }
        return trimmed;
    }

    private String buildConversationTitle(String reportType) {
        return switch (reportType) {
            case "PROHIBITED_CONTACT" -> "Report: prohibited listing";
            case "COUNTERFEIT" -> "Report: suspected counterfeit";
            case "WRONG_CATEGORY" -> "Report: wrong category";
            case "FRAUD_RISK" -> "Report: fraud risk";
            default -> "Report: other issue";
        };
    }
}
