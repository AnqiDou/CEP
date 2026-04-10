package cep_backend.controller;
import cep_backend.dto.AuthUserDto;
import cep_backend.service.AuthService;
import cep_backend.common.result.ApiResponse;
import cep_backend.dto.ItemDetailDto;
import cep_backend.dto.ItemFavoriteStatusDto;
import cep_backend.dto.ItemReportCreateRequest;
import cep_backend.dto.ItemReportCreateResultDto;
import cep_backend.service.ItemDetailService;
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
