package cep_backend.service;

import cep_backend.common.exception.BusinessException;
import cep_backend.service.AuthService;
import cep_backend.dto.HomeCategoryDto;
import cep_backend.dto.HomeItemDto;
import cep_backend.dto.HomeItemListDto;
import cep_backend.dto.HomeNoticeDto;
import cep_backend.dto.HotKeywordDto;
import cep_backend.entity.po.HomeCategoryRecord;
import cep_backend.entity.po.HomeItemRecord;
import cep_backend.mapper.HomeRepository;
import cep_backend.service.CreditLevelResolver;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class HomeService {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 50;

    private final HomeRepository homeRepository;
    private final AuthService authService;

    public HomeService(HomeRepository homeRepository, AuthService authService) {
        this.homeRepository = homeRepository;
        this.authService = authService;
    }

    public List<HomeCategoryDto> listCategories() {
        return homeRepository.findAllCategories().stream().map(this::mapCategory).toList();
    }

    public HomeItemListDto searchItems(String keyword,
            Long categoryId,
            String opsColumn,
            String viewerScope,
            String sortBy,
            String sortOrder,
            Integer page,
            Integer size,
            String authorizationHeader) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedOpsColumn = normalizeOpsColumn(opsColumn);
        Long currentUserId = resolveCurrentUserId(authorizationHeader);
        String normalizedViewerScope = normalizeViewerScope(viewerScope, currentUserId);

        String orderBy = resolveOrderBy(sortBy);
        String order = resolveOrder(sortOrder);
        if (categoryId != null) {
            orderBy = "i.created_at";
            order = "DESC";
        }
        int offset = (safePage - 1) * safeSize;
        boolean selfOnly = "self".equals(normalizedViewerScope);
        boolean othersOnly = "others".equals(normalizedViewerScope);
        boolean hotRecommendationMode = categoryId == null
                && normalizedKeyword.isBlank()
                && normalizedOpsColumn.isBlank();

        long total = homeRepository.countItems(
                normalizedKeyword,
                categoryId,
                normalizedOpsColumn,
                currentUserId,
                selfOnly,
                othersOnly);
        List<HomeItemDto> items = (hotRecommendationMode
                ? homeRepository.findItemsByHotPriority(
                        normalizedKeyword,
                        categoryId,
                        normalizedOpsColumn,
                        currentUserId,
                        selfOnly,
                        othersOnly,
                        offset,
                        safeSize)
                : homeRepository.findItems(
                        normalizedKeyword,
                        categoryId,
                        normalizedOpsColumn,
                        currentUserId,
                        selfOnly,
                        othersOnly,
                        orderBy,
                        order,
                        offset,
                        safeSize))
                .stream()
                .map(this::mapItem)
                .toList();

        if (!normalizedKeyword.isBlank()) {
            homeRepository.recordSearchKeyword(normalizedKeyword, LocalDateTime.now());
        }

        return new HomeItemListDto(items, total, safePage, safeSize);
    }

    public List<HomeItemDto> listHotItems(Integer limit) {
        return listHotItems(limit, null, null);
    }

    public List<HomeItemDto> listHotItems(Integer limit, String viewerScope, String authorizationHeader) {
        int safeLimit = normalizeLimit(limit);
        Long currentUserId = resolveCurrentUserId(authorizationHeader);
        String normalizedViewerScope = normalizeViewerScope(viewerScope, currentUserId);
        if ("self".equals(normalizedViewerScope)) {
            return homeRepository
                    .findItemsByHotPriority("", null, "", currentUserId, true, false, 0, safeLimit)
                    .stream()
                    .map(this::mapItem)
                    .toList();
        }
        if ("others".equals(normalizedViewerScope)) {
            return homeRepository
                    .findItemsByHotPriority("", null, "", currentUserId, false, true, 0, safeLimit)
                    .stream()
                    .map(this::mapItem)
                    .toList();
        }
        return homeRepository
                .findItemsByHotPriority("", null, "", currentUserId, false, false, 0, safeLimit)
                .stream()
                .map(this::mapItem)
                .toList();
    }

    public List<HotKeywordDto> listHotKeywords(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return homeRepository.findHotKeywords(safeLimit)
                .stream()
                .map(record -> new HotKeywordDto(record.keyword(), record.searchCount()))
                .toList();
    }

    public List<HomeNoticeDto> listHomeNotices(Integer limit) {
        int safeLimit = normalizeNoticeLimit(limit);
        return homeRepository.findHomeNotices(safeLimit);
    }

    private HomeCategoryDto mapCategory(HomeCategoryRecord record) {
        List<String> tags = Arrays.stream(record.tags().split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
        return new HomeCategoryDto(record.id(), record.code(), record.name(), record.description(), tags);
    }

    private HomeItemDto mapItem(HomeItemRecord record) {
        return new HomeItemDto(
                record.id(),
                record.categoryId(),
                record.publisherUserId(),
                Boolean.TRUE.equals(record.self()),
                record.sellerName(),
                record.sellerAvatarUrl(),
                CreditLevelResolver.resolveLabel(record.sellerCreditScore()),
                record.categoryCode(),
                record.categoryName(),
                record.title(),
                record.description(),
                record.price(),
                record.opsColumns(),
                record.photoUrl(),
                record.createdAt());
    }

    private Long resolveCurrentUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        try {
            return authService.currentUser(authorizationHeader).userId();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1) {
            throw new BusinessException("page 不能小于 1");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException("size 必须在 1 到 50 之间");
        }
        return size;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 8;
        }
        if (limit < 1 || limit > 20) {
            throw new BusinessException("limit 必须在 1 到 20 之间");
        }
        return limit;
    }

    private int normalizeNoticeLimit(Integer limit) {
        if (limit == null) {
            return 3;
        }
        if (limit < 1 || limit > 10) {
            throw new BusinessException("limit 必须在 1 到 10 之间");
        }
        return limit;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private String normalizeOpsColumn(String opsColumn) {
        if (opsColumn == null || opsColumn.trim().isEmpty()) {
            return "";
        }
        String value = opsColumn.trim().toLowerCase();
        return switch (value) {
            case "campus-bargain", "graduate-clearance", "back-to-school" -> value;
            default -> throw new BusinessException("opsColumn 不合法");
        };
    }

    private String resolveOrderBy(String sortBy) {
        String value = sortBy == null ? "time" : sortBy.trim().toLowerCase();
        return switch (value) {
            case "time" -> "i.created_at";
            case "price" -> "i.price";
            default -> throw new BusinessException("sortBy 仅支持 time 或 price");
        };
    }

    private String resolveOrder(String sortOrder) {
        String value = sortOrder == null ? "desc" : sortOrder.trim().toLowerCase();
        return switch (value) {
            case "asc" -> "ASC";
            case "desc" -> "DESC";
            default -> throw new BusinessException("sortOrder 仅支持 asc 或 desc");
        };
    }

    private String normalizeViewerScope(String viewerScope, Long currentUserId) {
        if (currentUserId == null) {
            return "all";
        }
        if (viewerScope == null || viewerScope.isBlank()) {
            return "all";
        }
        String value = viewerScope.trim().toLowerCase();
        return switch (value) {
            case "all", "self", "others" -> value;
            default -> throw new BusinessException("viewerScope 仅支持 all、self、others");
        };
    }
}
