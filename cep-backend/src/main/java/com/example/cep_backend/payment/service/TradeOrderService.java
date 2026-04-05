package com.example.cep_backend.payment.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.payment.dto.CreateTradeOrderRequest;
import com.example.cep_backend.payment.dto.TradeOrderDto;
import com.example.cep_backend.payment.model.TradeOrderItemSnapshot;
import com.example.cep_backend.payment.model.TradeOrderRecord;
import com.example.cep_backend.payment.repository.TradeOrderRepository;
import com.example.cep_backend.review.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TradeOrderService {
    private static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final DateTimeFormatter ORDER_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TradeOrderRepository tradeOrderRepository;
    private final ReviewService reviewService;

    public TradeOrderService(TradeOrderRepository tradeOrderRepository, ReviewService reviewService) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.reviewService = reviewService;
    }

    @Transactional
    public TradeOrderDto createOrder(Long buyerUserId, CreateTradeOrderRequest request) {
        if (buyerUserId == null || buyerUserId <= 0) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }
        if (request == null) {
            throw new BusinessException("订单信息无效");
        }

        Long itemId = request.itemId();
        if (itemId == null || itemId <= 0) {
            throw new BusinessException("物品信息无效");
        }

        String receiverName = requireText(request.receiverName(), "请填写收货人姓名");
        String receiverPhone = requireText(request.receiverPhone(), "请填写联系电话");
        String receiverAddress = requireText(request.receiverAddress(), "请填写收货地址");

        TradeOrderItemSnapshot snapshot = tradeOrderRepository.findPublishedItemSnapshot(itemId);
        if (snapshot == null) {
            throw new BusinessException("物品不存在或不可交易");
        }
        if (tradeOrderRepository.existsPendingOrderForItem(itemId)) {
            throw new BusinessException("该物品已有待付款订单，请先完成支付或取消后再下单");
        }
        if (snapshot.sellerUserId() == null || snapshot.sellerUserId() <= 0) {
            throw new BusinessException("物品发布者信息缺失，暂不可下单");
        }
        if (snapshot.sellerUserId().equals(buyerUserId)) {
            throw new BusinessException("不能购买自己发布的物品");
        }

        Long orderId = tradeOrderRepository.createOrder(
                generateOrderNo(),
                snapshot,
                buyerUserId,
                receiverName,
                receiverPhone,
                receiverAddress);
        if (orderId == null) {
            throw new BusinessException("订单创建失败，请稍后重试");
        }

        TradeOrderRecord order = tradeOrderRepository.findOrderById(orderId);
        if (order == null) {
            throw new BusinessException("订单创建失败，请稍后重试");
        }
        return toDto(order);
    }

    public TradeOrderDto getOrder(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException("订单信息无效");
        }

        TradeOrderRecord order = tradeOrderRepository.findOrderById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return toDto(order);
    }

    @Transactional
    public TradeOrderDto cancelOrder(Long actorUserId, Long orderId) {
        if (actorUserId == null || actorUserId <= 0) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }
        if (orderId == null || orderId <= 0) {
            throw new BusinessException("订单信息无效");
        }

        TradeOrderRecord order = tradeOrderRepository.findOrderById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        boolean canOperate = actorUserId.equals(order.buyerUserId()) || actorUserId.equals(order.sellerUserId());
        if (!canOperate) {
            throw new BusinessException("无权限操作该订单");
        }

        if (STATUS_CANCELLED.equals(order.status())) {
            return toDto(order);
        }

        if (!STATUS_PENDING_PAYMENT.equals(order.status())) {
            throw new BusinessException("当前订单状态不可取消");
        }

        int updatedRows = tradeOrderRepository.cancelPendingOrder(orderId, actorUserId);
        if (updatedRows <= 0) {
            throw new BusinessException("订单取消失败，请稍后重试");
        }

        TradeOrderRecord updated = tradeOrderRepository.findOrderById(orderId);
        if (updated == null) {
            throw new BusinessException("订单取消失败，请稍后重试");
        }
        return toDto(updated);
    }

    @Transactional
    public TradeOrderDto markOrderPaid(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException("订单信息无效");
        }

        TradeOrderRecord order = tradeOrderRepository.findOrderById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (STATUS_PAID.equals(order.status())) {
            return toDto(order);
        }

        if (!STATUS_PENDING_PAYMENT.equals(order.status())) {
            throw new BusinessException("当前订单状态不可支付");
        }

        tradeOrderRepository.markOrderPaid(orderId, LocalDateTime.now());
        TradeOrderRecord updated = tradeOrderRepository.findOrderById(orderId);
        if (updated == null) {
            throw new BusinessException("支付状态更新失败，请稍后重试");
        }

        reviewService.ensureReviewInvite(updated.id(), updated.itemId(), updated.buyerUserId(), updated.sellerUserId());
        return toDto(updated);
    }

    private TradeOrderDto toDto(TradeOrderRecord record) {
        return new TradeOrderDto(
                record.id(),
                record.orderNo(),
                record.itemId(),
                record.itemTitle(),
                record.amount(),
                record.coverPhotoUrl(),
                record.status(),
                record.receiverName(),
                record.receiverPhone(),
                record.receiverAddress(),
                record.createdAt(),
                record.paidAt());
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String generateOrderNo() {
        String timePart = LocalDateTime.now().format(ORDER_NO_TIME_FORMATTER);
        int randomPart = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "CEP" + timePart + randomPart;
    }
}
