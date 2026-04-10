package cep_backend.controller;
import cep_backend.dto.AuthUserDto;
import cep_backend.service.AuthService;
import cep_backend.common.result.ApiResponse;
import cep_backend.dto.ReviewOrderDetailDto;
import cep_backend.dto.SubmitReviewRequest;
import cep_backend.dto.SubmitReviewResultDto;
import cep_backend.service.ReviewService;
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
