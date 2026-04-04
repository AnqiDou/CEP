package com.example.cep_backend.home.controller;

import com.example.cep_backend.common.api.ApiResponse;
import com.example.cep_backend.home.dto.HomeCategoryDto;
import com.example.cep_backend.home.dto.HomeItemDto;
import com.example.cep_backend.home.dto.HomeItemListDto;
import com.example.cep_backend.home.dto.HotKeywordDto;
import com.example.cep_backend.home.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
public class HomeController {
    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<HomeCategoryDto>> categories() {
        return ApiResponse.ok("获取成功", homeService.listCategories());
    }

    @GetMapping("/items")
    public ApiResponse<HomeItemListDto> items(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String opsColumn,
            @RequestParam(required = false) String viewerScope,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        HomeItemListDto list = homeService.searchItems(
                keyword,
                categoryId,
                opsColumn,
                viewerScope,
                sortBy,
                sortOrder,
                page,
                size,
                authorization);
        return ApiResponse.ok("获取成功", list);
    }

    @GetMapping("/hot-items")
    public ApiResponse<List<HomeItemDto>> hotItems(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String viewerScope,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok("获取成功", homeService.listHotItems(limit, viewerScope, authorization));
    }

    @GetMapping("/hot-keywords")
    public ApiResponse<List<HotKeywordDto>> hotKeywords(@RequestParam(required = false) Integer limit) {
        return ApiResponse.ok("获取成功", homeService.listHotKeywords(limit));
    }
}
