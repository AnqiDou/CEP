package com.example.cep_backend.review.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.review.dto.ReviewOrderDetailDto;
import com.example.cep_backend.review.dto.SubmitReviewRequest;
import com.example.cep_backend.review.dto.SubmitReviewResultDto;
import com.example.cep_backend.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ReviewOrderDetailDto getOrderDetail(Long reviewerUserId, Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException("订单参数无效");
        }
        ReviewRepository.ReviewTaskDetail detail = reviewRepository.findReviewTaskDetail(orderId, reviewerUserId);
        if (detail == null) {
            throw new BusinessException("暂无待评价记录");
        }
        String status = normalizeStatus(detail.status());
        return new ReviewOrderDetailDto(
                detail.orderId(),
                detail.itemId(),
                detail.itemTitle(),
                detail.itemCover(),
                detail.targetUserId(),
                detail.targetUserName(),
                detail.targetRole(),
                status,
                "PENDING".equals(status));
    }

    @Transactional
    public SubmitReviewResultDto submitReview(Long reviewerUserId, Long orderId, SubmitReviewRequest request) {
        if (request == null) {
            throw new BusinessException("评价参数无效");
        }
        String normalizedRating = normalizeRating(request.rating());
        String normalizedContent = normalizeContent(request.content());

        ReviewRepository.ReviewTaskDetail detail = reviewRepository.findReviewTaskDetail(orderId, reviewerUserId);
        if (detail == null) {
            throw new BusinessException("暂无可提交的评价");
        }
        String status = normalizeStatus(detail.status());
        if (!"PENDING".equals(status)) {
            throw new BusinessException("该订单已评价，请勿重复提交");
        }

        reviewRepository.submitReview(
                detail.orderId(),
                detail.reviewerUserId(),
                detail.targetUserId(),
                detail.targetRole(),
                normalizedRating,
                normalizedContent,
                LocalDateTime.now());
        return new SubmitReviewResultDto(detail.orderId(), "SUBMITTED");
    }

    public void ensureReviewInvite(Long orderId, Long itemId, Long buyerUserId, Long sellerUserId) {
        reviewRepository.ensureReviewInvite(orderId, itemId, buyerUserId, sellerUserId);
    }

    private String normalizeRating(String rating) {
        String value = rating == null ? "" : rating.trim().toLowerCase();
        if (!"good".equals(value) && !"bad".equals(value)) {
            throw new BusinessException("评价结果仅支持 good / bad");
        }
        return value;
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        String value = content.trim();
        if (value.length() > 300) {
            throw new BusinessException("评价内容不能超过300字");
        }
        return value;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }
        return status.trim().toUpperCase();
    }
}
