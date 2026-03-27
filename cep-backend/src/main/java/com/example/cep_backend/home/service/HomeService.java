package com.example.cep_backend.home.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.home.dto.HomeCategoryDto;
import com.example.cep_backend.home.dto.HomeItemDto;
import com.example.cep_backend.home.dto.HomeItemListDto;
import com.example.cep_backend.home.dto.HotKeywordDto;
import com.example.cep_backend.home.model.HomeCategoryRecord;
import com.example.cep_backend.home.model.HomeItemRecord;
import com.example.cep_backend.home.repository.HomeRepository;
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

    public HomeService(HomeRepository homeRepository) {
        this.homeRepository = homeRepository;
    }

    public List<HomeCategoryDto> listCategories() {
        return homeRepository.findAllCategories().stream().map(this::mapCategory).toList();
    }

    public HomeItemListDto searchItems(String keyword,
            Long categoryId,
            String sortBy,
            String sortOrder,
            Integer page,
            Integer size) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        String normalizedKeyword = normalizeKeyword(keyword);

        String orderBy = resolveOrderBy(sortBy);
        String order = resolveOrder(sortOrder);
        int offset = (safePage - 1) * safeSize;

        long total = homeRepository.countItems(normalizedKeyword, categoryId);
        List<HomeItemDto> items = homeRepository
                .findItems(normalizedKeyword, categoryId, orderBy, order, offset, safeSize)
                .stream()
                .map(this::mapItem)
                .toList();

        if (!normalizedKeyword.isBlank()) {
            homeRepository.recordSearchKeyword(normalizedKeyword, LocalDateTime.now());
        }

        return new HomeItemListDto(items, total, safePage, safeSize);
    }

    public List<HomeItemDto> listHotItems(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return homeRepository.findHotItems(safeLimit).stream().map(this::mapItem).toList();
    }

    public List<HotKeywordDto> listHotKeywords(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return homeRepository.findHotKeywords(safeLimit)
                .stream()
                .map(record -> new HotKeywordDto(record.keyword(), record.searchCount()))
                .toList();
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
                record.categoryCode(),
                record.categoryName(),
                record.title(),
                record.description(),
                record.price(),
                record.campus(),
                record.badge(),
                record.photoUrl(),
                record.createdAt());
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

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
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
}
