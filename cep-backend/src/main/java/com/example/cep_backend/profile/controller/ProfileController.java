package com.example.cep_backend.profile.controller;

import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.service.AuthService;
import com.example.cep_backend.common.api.ApiResponse;
import com.example.cep_backend.profile.dto.ProfileAvatarUploadDto;
import com.example.cep_backend.profile.dto.OtherProfileItemDto;
import com.example.cep_backend.profile.dto.OtherProfileOverviewDto;
import com.example.cep_backend.profile.dto.OtherProfileReviewSummaryDto;
import com.example.cep_backend.profile.dto.ProfileFollowUserDto;
import com.example.cep_backend.profile.dto.ProfileOverviewDto;
import com.example.cep_backend.profile.dto.ProfilePendingTradeDto;
import com.example.cep_backend.profile.dto.ProfileReviewSummaryDto;
import com.example.cep_backend.profile.dto.ProfileTradeContactDto;
import com.example.cep_backend.profile.dto.ProfileTradeItemDto;
import com.example.cep_backend.profile.dto.ProfileUpdateRequest;
import com.example.cep_backend.profile.service.ProfileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final AuthService authService;

    public ProfileController(ProfileService profileService, AuthService authService) {
        this.profileService = profileService;
        this.authService = authService;
    }

    @GetMapping("/overview")
    public ApiResponse<ProfileOverviewDto> getOverview(@RequestHeader("Authorization") String authorization) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getOverview(user.userId()));
    }

    @GetMapping("/reviews")
    public ApiResponse<ProfileReviewSummaryDto> getReviews(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "all") String rating) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getReviews(user.userId(), rating));
    }

    @GetMapping("/trades/published")
    public ApiResponse<List<ProfileTradeItemDto>> getPublished(@RequestHeader("Authorization") String authorization) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getPublishedItems(user.userId()));
    }

    @GetMapping("/trades/sold")
    public ApiResponse<List<ProfileTradeItemDto>> getSold(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "all") String status) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getSoldItems(user.userId(), status));
    }

    @GetMapping("/trades/bought")
    public ApiResponse<List<ProfileTradeItemDto>> getBought(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "all") String status) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getBoughtItems(user.userId(), status));
    }

    @GetMapping("/favorites")
    public ApiResponse<List<ProfileTradeItemDto>> getFavorites(@RequestHeader("Authorization") String authorization) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getFavoriteItems(user.userId()));
    }

    @GetMapping("/following")
    public ApiResponse<List<ProfileFollowUserDto>> getFollowing(@RequestHeader("Authorization") String authorization) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getFollowingUsers(user.userId()));
    }

    @GetMapping("/fans")
    public ApiResponse<List<ProfileFollowUserDto>> getFans(@RequestHeader("Authorization") String authorization) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getFansUsers(user.userId()));
    }

    @GetMapping("/trades/pending-payment")
    public ApiResponse<List<ProfilePendingTradeDto>> getPendingPayment(
            @RequestHeader("Authorization") String authorization) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getPendingPaymentTrades(user.userId()));
    }

    @GetMapping("/trades/sold/{orderId}/contact")
    public ApiResponse<ProfileTradeContactDto> getSoldOrderBuyerContact(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long orderId) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getSoldOrderBuyerContact(user.userId(), orderId));
    }

    @GetMapping("/trades/bought/{orderId}/contact")
    public ApiResponse<ProfileTradeContactDto> getBoughtOrderSellerContact(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long orderId) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getBoughtOrderSellerContact(user.userId(), orderId));
    }

    @PostMapping("/trades/bought/{orderId}/rebuy")
    public ApiResponse<ProfileTradeItemDto> rebuyBoughtOrder(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long orderId) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.rebuyItem(user.userId(), orderId));
    }

    @PutMapping("/basic")
    public ApiResponse<ProfileOverviewDto> updateBasic(
            @RequestHeader("Authorization") String authorization,
            @RequestBody ProfileUpdateRequest request) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("更新成功", profileService.updateBasicInfo(user.userId(), request));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileAvatarUploadDto> uploadAvatar(
            @RequestHeader("Authorization") String authorization,
            @RequestPart("file") MultipartFile file) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("上传成功", profileService.uploadAvatar(user.userId(), file));
    }

    @GetMapping("/other/overview")
    public ApiResponse<OtherProfileOverviewDto> getOtherOverview(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username) {
        AuthUserDto viewer = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getOtherOverview(viewer.userId(), userId, username));
    }

    @GetMapping("/other/items")
    public ApiResponse<List<OtherProfileItemDto>> getOtherItems(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "price-desc") String sort) {
        AuthUserDto viewer = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getOtherItems(viewer.userId(), userId, username, status, sort));
    }

    @GetMapping("/other/reviews")
    public ApiResponse<OtherProfileReviewSummaryDto> getOtherReviews(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "all") String rating) {
        AuthUserDto viewer = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", profileService.getOtherReviews(viewer.userId(), userId, username, rating));
    }

    @PostMapping("/other/follow")
    public ApiResponse<Void> followOther(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username) {
        AuthUserDto viewer = authService.currentUser(authorization);
        profileService.followOther(viewer.userId(), userId, username);
        return ApiResponse.ok("关注成功", null);
    }

    @DeleteMapping("/other/follow")
    public ApiResponse<Void> unfollowOther(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username) {
        AuthUserDto viewer = authService.currentUser(authorization);
        profileService.unfollowOther(viewer.userId(), userId, username);
        return ApiResponse.ok("取消关注成功", null);
    }
}
