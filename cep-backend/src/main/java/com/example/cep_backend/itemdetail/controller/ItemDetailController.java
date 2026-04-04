package com.example.cep_backend.itemdetail.controller;

import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.service.AuthService;
import com.example.cep_backend.common.api.ApiResponse;
import com.example.cep_backend.itemdetail.dto.ItemDetailDto;
import com.example.cep_backend.itemdetail.dto.ItemFavoriteStatusDto;
import com.example.cep_backend.itemdetail.dto.ItemReportCreateRequest;
import com.example.cep_backend.itemdetail.dto.ItemReportCreateResultDto;
import com.example.cep_backend.itemdetail.service.ItemDetailService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class ItemDetailController {
    private final ItemDetailService itemDetailService;
    private final AuthService authService;

    public ItemDetailController(ItemDetailService itemDetailService, AuthService authService) {
        this.itemDetailService = itemDetailService;
        this.authService = authService;
    }

    @GetMapping("/{itemId}/detail")
    public ApiResponse<ItemDetailDto> detail(@PathVariable Long itemId) {
        return ApiResponse.ok("获取成功", itemDetailService.getItemDetail(itemId));
    }

    @GetMapping("/{itemId}/favorite")
    public ApiResponse<ItemFavoriteStatusDto> favoriteStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long itemId) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", itemDetailService.getFavoriteStatus(user.userId(), itemId));
    }

    @PostMapping("/{itemId}/favorite")
    public ApiResponse<ItemFavoriteStatusDto> addFavorite(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long itemId) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("收藏成功", itemDetailService.addFavorite(user.userId(), itemId));
    }

    @DeleteMapping("/{itemId}/favorite")
    public ApiResponse<ItemFavoriteStatusDto> removeFavorite(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long itemId) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("取消收藏成功", itemDetailService.removeFavorite(user.userId(), itemId));
    }

    @PostMapping("/{itemId}/reports")
    public ApiResponse<ItemReportCreateResultDto> createReport(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long itemId,
            @RequestBody ItemReportCreateRequest request) {
        AuthUserDto user = authService.currentUser(authorization);
        ItemReportCreateResultDto result = itemDetailService.createReport(
                user.userId(),
                itemId,
                request == null ? null : request.reportType(),
                request == null ? null : request.content());
        return ApiResponse.ok("提交成功", result);
    }
}
