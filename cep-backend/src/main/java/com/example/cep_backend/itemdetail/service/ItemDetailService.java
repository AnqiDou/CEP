package com.example.cep_backend.itemdetail.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.itemdetail.dto.ItemDetailDto;
import com.example.cep_backend.itemdetail.dto.ItemFavoriteStatusDto;
import com.example.cep_backend.itemdetail.dto.ItemReportCreateResultDto;
import com.example.cep_backend.itemdetail.dto.ItemDetailPublisherDto;
import com.example.cep_backend.itemdetail.model.ItemDetailRecord;
import com.example.cep_backend.itemdetail.repository.ItemDetailRepository;
import com.example.cep_backend.message.service.MessageNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ItemDetailService {
    private static final String DEFAULT_USAGE_DURATION = "未填写";
    private static final String DEFAULT_CONDITION = "以实物为准";
    private static final String DEFAULT_ACCESSORIES = "以实际交易信息为准";
    private static final String DEFAULT_NOTE = "支持当面交易，注意安全。";
    private static final String DEFAULT_LOCATION = "未填写";
    private static final String DEFAULT_PUBLISHER_NAME = "校园用户";
    private static final String DEFAULT_PUBLISHER_COLLEGE = "未填写学院";
    private static final String DEFAULT_PUBLISHER_NOTE = "该用户暂未完善个人简介";

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

        String campus = normalizeText(record.campus(), DEFAULT_LOCATION);
        BigDecimal originalPrice = resolveOriginalPrice(record.price(), record.originalPrice());

        ItemDetailPublisherDto publisher = new ItemDetailPublisherDto(
                record.publisherId(),
                normalizeText(record.publisherName(), DEFAULT_PUBLISHER_NAME),
                normalizeText(record.publisherCollege(), DEFAULT_PUBLISHER_COLLEGE),
                normalizeText(record.publisherCampus(), campus),
                resolvePublisherCredit(record.publisherCredit()),
                normalizeText(record.publisherNote(), DEFAULT_PUBLISHER_NOTE));

        return new ItemDetailDto(
                record.itemId(),
                record.categoryId(),
                record.categoryCode(),
                record.categoryName(),
                record.title(),
                record.price(),
                originalPrice,
                record.purchaseDate(),
                normalizeText(record.usageDuration(), DEFAULT_USAGE_DURATION),
                normalizeText(record.tradeLocation(), campus),
                record.createdAt(),
                normalizeText(record.itemCondition(), DEFAULT_CONDITION),
                normalizeText(record.accessories(), DEFAULT_ACCESSORIES),
                record.description(),
                normalizeText(record.detailNote(), DEFAULT_NOTE),
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

    private BigDecimal resolveOriginalPrice(BigDecimal price, BigDecimal originalPrice) {
        BigDecimal fallback = price.multiply(new BigDecimal("2")).setScale(2, RoundingMode.HALF_UP);
        if (originalPrice == null) {
            return fallback;
        }
        BigDecimal normalized = originalPrice.setScale(2, RoundingMode.HALF_UP);
        return normalized.compareTo(price) < 0 ? fallback : normalized;
    }

    private BigDecimal resolvePublisherCredit(BigDecimal credit) {
        if (credit == null) {
            return new BigDecimal("4.5");
        }
        return credit.setScale(1, RoundingMode.HALF_UP);
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
