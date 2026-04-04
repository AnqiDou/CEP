package com.example.cep_backend.itemdetail.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.itemdetail.dto.ItemDetailDto;
import com.example.cep_backend.itemdetail.dto.ItemFavoriteStatusDto;
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
}
