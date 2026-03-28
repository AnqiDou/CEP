package com.example.cep_backend.review.controller;

import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.service.AuthService;
import com.example.cep_backend.common.api.ApiResponse;
import com.example.cep_backend.review.dto.ReviewOrderDetailDto;
import com.example.cep_backend.review.dto.SubmitReviewRequest;
import com.example.cep_backend.review.dto.SubmitReviewResultDto;
import com.example.cep_backend.review.service.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final AuthService authService;

    public ReviewController(ReviewService reviewService, AuthService authService) {
        this.reviewService = reviewService;
        this.authService = authService;
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<ReviewOrderDetailDto> getOrderReviewDetail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long orderId) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", reviewService.getOrderDetail(user.userId(), orderId));
    }

    @PostMapping("/orders/{orderId}")
    public ApiResponse<SubmitReviewResultDto> submitReview(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long orderId,
            @RequestBody SubmitReviewRequest request) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("评价成功", reviewService.submitReview(user.userId(), orderId, request));
    }
}
