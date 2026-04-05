package com.example.cep_backend.publish.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.message.service.MessageNotificationService;
import com.example.cep_backend.publish.dto.PublishItemDto;
import com.example.cep_backend.publish.dto.PublishItemRequest;
import com.example.cep_backend.publish.dto.PublishItemUpdateRequest;
import com.example.cep_backend.publish.dto.PublishOwnedItemDto;
import com.example.cep_backend.publish.repository.PublishRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.List;
import java.util.Set;

@Service
public class PublishService {
    private static final int MAX_PHOTO_COUNT = 6;
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFF_SHELF = "OFF_SHELF";
    private static final String STATUS_DELETED = "DELETED";
    private static final BigDecimal CAMPUS_BARGAIN_MAX_PRICE = BigDecimal.valueOf(15);
    private static final Set<String> GRADUATE_CLEARANCE_CATEGORY_CODES = Set.of("book", "daily", "clothes", "sports");
    private static final Set<String> BACK_TO_SCHOOL_CATEGORY_CODES = Set.of("stationery", "daily", "digital", "book");
    private static final List<String> GRADUATE_CLEARANCE_KEYWORDS = Arrays.asList(
            "考研", "考公", "四六级", "教材", "复习资料",
            "小风扇", "台灯", "电饭煲", "吹风机",
            "床品", "衣柜", "书桌", "椅子", "行李箱");
    private static final List<String> BACK_TO_SCHOOL_KEYWORDS = Arrays.asList(
            "床垫", "床帘", "收纳箱", "洗漱", "暖壶",
            "笔记本", "文具", "四六级", "计算器",
            "军训", "书包", "水杯", "雨伞",
            "耳机", "键盘", "鼠标", "电脑支架", "插排");

    private final PublishRepository publishRepository;
    private final MessageNotificationService messageNotificationService;

    public PublishService(PublishRepository publishRepository, MessageNotificationService messageNotificationService) {
        this.publishRepository = publishRepository;
        this.messageNotificationService = messageNotificationService;
    }

    @Transactional
    public PublishItemDto publishItem(Long userId, PublishItemRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户信息无效，请重新登录");
        }

        String itemName = validateText(request.name(), "物品名称", 120);
        String categoryCode = normalizeCategoryCode(request.categoryCode());
        BigDecimal price = validatePrice(request.price());
        LocalDate purchaseDate = normalizePurchaseDate(request.purchaseDate());
        String usageDuration = normalizeOptionalText(request.usageDuration(), "使用时长", 50);
        String description = normalizeOptionalText(request.description(), "物品描述", 500);
        List<String> photoUrls = validatePhotoUrls(request.photoUrls());

        Long categoryId = publishRepository.findCategoryIdByCode(categoryCode);
        if (categoryId == null) {
            throw new BusinessException("分类不存在，请重新选择");
        }

        LocalDateTime now = LocalDateTime.now();
        Long itemId;
        try {
            itemId = publishRepository.insertItem(
                    categoryId,
                    userId,
                    itemName,
                    price,
                    description,
                    now);
            publishRepository.insertItemPhotos(itemId, photoUrls, now);
            publishRepository.insertItemDetail(itemId, userId, purchaseDate, usageDuration, now);
            refreshItemOpsColumns(itemId, categoryId, categoryCode, itemName, price, description, usageDuration, now);
        } catch (DataAccessException ex) {
            throw new BusinessException("发布失败：请先确认已执行建表脚本并检查数据库字段");
        }

        return new PublishItemDto(
                itemId,
                itemName,
                categoryCode,
                price,
                purchaseDate,
                usageDuration,
                description,
                photoUrls,
                now);
    }

    public List<PublishOwnedItemDto> getMyItems(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户信息无效，请重新登录");
        }
        return publishRepository.findMyItems(userId).stream()
                .map(item -> toOwnedItemDto(item, publishRepository.findPhotoUrlsByItemId(item.id())))
                .toList();
    }

    @Transactional
    public PublishOwnedItemDto updateMyItem(Long userId, Long itemId, PublishItemUpdateRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户信息无效，请重新登录");
        }
        if (itemId == null || itemId <= 0) {
            throw new BusinessException("物品信息无效");
        }
        if (request == null) {
            throw new BusinessException("物品参数无效");
        }

        PublishRepository.PublishOwnedItemBaseRecord existing = publishRepository.findOwnedItem(userId, itemId)
                .orElseThrow(() -> new BusinessException("物品不存在或无操作权限"));
        if (STATUS_DELETED.equals(existing.status())) {
            throw new BusinessException("已删除物品不可编辑");
        }

        String itemName = validateText(request.name(), "物品名称", 120);
        String categoryCode = normalizeCategoryCode(request.categoryCode());
        BigDecimal price = validatePrice(request.price());
        LocalDate purchaseDate = normalizePurchaseDate(request.purchaseDate());
        String usageDuration = normalizeOptionalText(request.usageDuration(), "使用时长", 50);
        String description = normalizeOptionalText(request.description(), "物品描述", 500);
        List<String> photoUrls = validatePhotoUrls(request.photoUrls());

        Long categoryId = publishRepository.findCategoryIdByCode(categoryCode);
        if (categoryId == null) {
            throw new BusinessException("分类不存在，请重新选择");
        }

        LocalDateTime now = LocalDateTime.now();
        publishRepository.updateItemAndDetail(userId, itemId, categoryId, itemName, price, description, purchaseDate,
                usageDuration, now);
        publishRepository.replaceItemPhotos(itemId, photoUrls, now);
        refreshItemOpsColumns(itemId, categoryId, categoryCode, itemName, price, description, usageDuration, now);

        messageNotificationService.notifyFavoritePriceDrop(itemId, userId, existing.price(), price);

        PublishRepository.PublishOwnedItemBaseRecord updated = publishRepository.findOwnedItem(userId, itemId)
                .orElseThrow(() -> new BusinessException("物品不存在或无操作权限"));
        return toOwnedItemDto(updated, publishRepository.findPhotoUrlsByItemId(itemId));
    }

    @Transactional
    public void deleteMyItem(Long userId, Long itemId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户信息无效，请重新登录");
        }
        if (itemId == null || itemId <= 0) {
            throw new BusinessException("物品信息无效");
        }
        int updated = publishRepository.markDeleted(userId, itemId, LocalDateTime.now());
        if (updated <= 0) {
            throw new BusinessException("物品不存在或无操作权限");
        }
    }

    @Transactional
    public PublishOwnedItemDto updateMyItemStatus(Long userId, Long itemId, String status) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户信息无效，请重新登录");
        }
        if (itemId == null || itemId <= 0) {
            throw new BusinessException("物品信息无效");
        }

        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!STATUS_PUBLISHED.equals(normalized) && !STATUS_OFF_SHELF.equals(normalized)) {
            throw new BusinessException("状态无效，仅支持 PUBLISHED / OFF_SHELF");
        }

        int updated = publishRepository.updateStatus(userId, itemId, normalized, LocalDateTime.now());
        if (updated <= 0) {
            throw new BusinessException("物品不存在、已删除或无操作权限");
        }
        PublishRepository.PublishOwnedItemBaseRecord item = publishRepository.findOwnedItem(userId, itemId)
                .orElseThrow(() -> new BusinessException("物品不存在或无操作权限"));

        if (STATUS_PUBLISHED.equals(normalized)) {
            LocalDateTime now = LocalDateTime.now();
            Long categoryId = publishRepository.findCategoryIdByCode(item.categoryCode());
            if (categoryId != null) {
                refreshItemOpsColumns(
                        item.id(),
                        categoryId,
                        item.categoryCode(),
                        item.name(),
                        item.price(),
                        item.description(),
                        item.usageDuration(),
                        now);
            }
        }

        return toOwnedItemDto(item, publishRepository.findPhotoUrlsByItemId(itemId));
    }

    private void refreshItemOpsColumns(Long itemId,
            Long categoryId,
            String categoryCode,
            String itemName,
            BigDecimal price,
            String description,
            String usageDuration,
            LocalDateTime now) {
        List<String> matchedColumns = new ArrayList<>();

        if (matchCampusBargain(price)) {
            matchedColumns.add("campus-bargain");
        }
        if (matchGraduateClearance(categoryCode, itemName, description)) {
            matchedColumns.add("graduate-clearance");
        }
        if (matchBackToSchool(categoryCode, itemName, description, usageDuration)) {
            matchedColumns.add("back-to-school");
        }

        publishRepository.replaceItemOpsColumns(itemId, matchedColumns, now);
    }

    private boolean matchCampusBargain(BigDecimal price) {
        return price != null && price.compareTo(CAMPUS_BARGAIN_MAX_PRICE) <= 0;
    }

    private boolean matchGraduateClearance(String categoryCode, String itemName, String description) {
        String normalizedCategoryCode = categoryCode == null ? "" : categoryCode.trim().toLowerCase(Locale.ROOT);
        if (GRADUATE_CLEARANCE_CATEGORY_CODES.contains(normalizedCategoryCode)) {
            return true;
        }
        String text = buildSearchText(itemName, description);
        return containsAnyKeyword(text, GRADUATE_CLEARANCE_KEYWORDS);
    }

    private boolean matchBackToSchool(String categoryCode, String itemName, String description, String usageDuration) {
        if (categoryCode == null) {
            return false;
        }
        String normalizedCategoryCode = categoryCode.trim().toLowerCase(Locale.ROOT);
        if (!BACK_TO_SCHOOL_CATEGORY_CODES.contains(normalizedCategoryCode)) {
            return false;
        }
        String text = buildSearchText(itemName, description, usageDuration);
        return containsAnyKeyword(text, BACK_TO_SCHOOL_KEYWORDS);
    }

    private String buildSearchText(String... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value.trim().toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private boolean containsAnyKeyword(String text, List<String> keywords) {
        if (text == null || text.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        return keywords.stream().anyMatch(keyword -> keyword != null && !keyword.isBlank() && text.contains(keyword));
    }

    private PublishOwnedItemDto toOwnedItemDto(PublishRepository.PublishOwnedItemBaseRecord item,
            List<String> photoUrls) {
        return new PublishOwnedItemDto(
                item.id(),
                item.name(),
                item.categoryCode(),
                item.price(),
                item.purchaseDate(),
                item.usageDuration(),
                item.description(),
                photoUrls,
                item.status(),
                item.createdAt());
    }

    private String normalizeCategoryCode(String categoryCode) {
        String value = categoryCode == null ? "other" : categoryCode.trim().toLowerCase();
        return value.isEmpty() ? "other" : value;
    }

    private LocalDate normalizePurchaseDate(LocalDate purchaseDate) {
        if (purchaseDate == null) {
            return null;
        }
        if (purchaseDate.isAfter(LocalDate.now())) {
            throw new BusinessException("购买时间不能晚于当前日期");
        }
        return purchaseDate;
    }

    private BigDecimal validatePrice(BigDecimal price) {
        if (price == null) {
            throw new BusinessException("请填写价格");
        }
        if (price.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new BusinessException("价格超出允许范围");
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> validatePhotoUrls(List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            return List.of();
        }
        if (photoUrls.size() > MAX_PHOTO_COUNT) {
            throw new BusinessException("最多上传 6 张图片");
        }

        List<String> normalized = photoUrls.stream().map(url -> {
            if (url == null || url.trim().isEmpty()) {
                throw new BusinessException("图片地址不能为空");
            }
            String value = url.trim();
            if (value.length() > 500) {
                throw new BusinessException("图片地址长度不能超过 500");
            }
            return value;
        }).toList();

        if (normalized.stream().distinct().count() != normalized.size()) {
            throw new BusinessException("图片不能重复上传");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException(fieldName + "长度不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    private String validateText(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("请填写" + fieldName);
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException(fieldName + "长度不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }
}
