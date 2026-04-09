package com.example.cep_backend.payment.service;

import com.example.cep_backend.auth.BusinessException;
import com.example.cep_backend.payment.dto.CreateTradeOrderRequest;
import com.example.cep_backend.payment.dto.TradeOrderDetailDto;
import com.example.cep_backend.payment.dto.TradeOrderDto;
import com.example.cep_backend.payment.model.TradeOrderItemSnapshot;
import com.example.cep_backend.payment.model.TradeOrderRecord;
import com.example.cep_backend.payment.repository.TradeOrderRepository;
import com.example.cep_backend.review.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TradeOrderService {
    private static final String ORDER_NO_PREFIX = "CEP";
    private static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String REFUND_STATUS_NONE = "NONE";
    private static final String REFUND_STATUS_APPLIED = "APPLIED";
    private static final String REFUND_STATUS_REJECTED = "REJECTED";
    private static final String REFUND_TYPE_NO_RECEIPT = "NO_RECEIPT";
    private static final String REFUND_TYPE_RETURN_AFTER_RECEIPT = "RETURN_AFTER_RECEIPT";
    private static final DateTimeFormatter ORDER_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

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
        Integer remainingQuantity = snapshot.remainingQuantity();
        if (remainingQuantity != null && remainingQuantity <= 0) {
            throw new BusinessException("该物品已售罄");
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

    public TradeOrderDetailDto getOrderDetail(Long actorUserId, Long orderId) {
        if (actorUserId == null || actorUserId <= 0) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }
        if (orderId == null || orderId <= 0) {
            throw new BusinessException("订单信息无效");
        }

        TradeOrderRepository.TradeOrderDetailRecord record = tradeOrderRepository.findOrderDetailByIdForUser(orderId,
                actorUserId);
        if (record == null) {
            throw new BusinessException("订单不存在或无权限查看");
        }

        return new TradeOrderDetailDto(
                record.orderId(),
                record.orderNo(),
                record.status(),
                mapOrderDetailStatusText(record.status(), record.refundStatus()),
                record.createdAt(),
                record.paidAt(),
                record.completedAt(),
                record.itemId(),
                record.itemTitle(),
                record.itemDescription(),
                record.itemImage(),
                record.amount(),
                record.buyerUserId(),
                record.buyerName(),
                record.buyerAvatar(),
                record.buyerPhone(),
                record.sellerUserId(),
                record.sellerName(),
                record.sellerAvatar(),
                record.sellerPhone());
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

        if (STATUS_PENDING_CONFIRMATION.equals(order.status()) || STATUS_COMPLETED.equals(order.status())) {
            return toDto(order);
        }

        if (!STATUS_PENDING_PAYMENT.equals(order.status())) {
            throw new BusinessException("当前订单状态不可支付");
        }

        if (!tradeOrderRepository.consumeOneStockOnPaid(order.itemId())) {
            throw new BusinessException("该物品库存不足或已下架，无法支付");
        }

        tradeOrderRepository.markOrderPaid(orderId, LocalDateTime.now());
        TradeOrderRecord updated = tradeOrderRepository.findOrderById(orderId);
        if (updated == null) {
            throw new BusinessException("支付状态更新失败，请稍后重试");
        }
        return toDto(updated);
    }

    @Transactional
    public TradeOrderDto confirmSellerDelivered(Long actorUserId, Long orderId) {
        TradeOrderRecord order = requireOrderActorAndPendingConfirmation(actorUserId, orderId);
        if (!actorUserId.equals(order.sellerUserId())) {
            throw new BusinessException("仅卖家可确认已交付物品");
        }
        if (REFUND_STATUS_APPLIED.equals(order.refundStatus())) {
            throw new BusinessException("当前订单已申请退款，无法确认交付");
        }

        tradeOrderRepository.markSellerConfirmedDelivery(orderId);
        tradeOrderRepository.completeOrderWhenBothConfirmed(orderId);

        TradeOrderRecord updated = tradeOrderRepository.findOrderById(orderId);
        if (updated == null) {
            throw new BusinessException("订单状态更新失败，请稍后重试");
        }
        if (STATUS_COMPLETED.equals(updated.status())) {
            reviewService.ensureReviewInvite(updated.id(), updated.itemId(), updated.buyerUserId(),
                    updated.sellerUserId());
        }
        return toDto(updated);
    }

    @Transactional
    public TradeOrderDto confirmBuyerReceived(Long actorUserId, Long orderId) {
        TradeOrderRecord order = requireOrderActorAndPendingConfirmation(actorUserId, orderId);
        if (!actorUserId.equals(order.buyerUserId())) {
            throw new BusinessException("仅买家可确认已收到物品");
        }
        if (REFUND_STATUS_APPLIED.equals(order.refundStatus())) {
            throw new BusinessException("当前订单已申请退款，无法确认收货");
        }

        tradeOrderRepository.markBuyerConfirmedReceived(orderId);
        tradeOrderRepository.completeOrderWhenBothConfirmed(orderId);

        TradeOrderRecord updated = tradeOrderRepository.findOrderById(orderId);
        if (updated == null) {
            throw new BusinessException("订单状态更新失败，请稍后重试");
        }
        if (STATUS_COMPLETED.equals(updated.status())) {
            reviewService.ensureReviewInvite(updated.id(), updated.itemId(), updated.buyerUserId(),
                    updated.sellerUserId());
        }
        return toDto(updated);
    }

    @Transactional
    public TradeOrderDto applyRefund(Long actorUserId, Long orderId, String refundType) {
        TradeOrderRecord order = requireOrderActorAndPendingConfirmation(actorUserId, orderId);
        if (!actorUserId.equals(order.buyerUserId())) {
            throw new BusinessException("仅买家可申请退款");
        }
        if (Boolean.TRUE.equals(order.buyerConfirmed())) {
            throw new BusinessException("买家已确认收货，无法申请退款");
        }
        String normalizedType = normalizeRefundType(refundType);
        int updatedRows = tradeOrderRepository.applyRefund(orderId, normalizedType);
        if (updatedRows <= 0) {
            throw new BusinessException("退款申请失败，订单可能已在处理中");
        }

        TradeOrderRecord updated = tradeOrderRepository.findOrderById(orderId);
        if (updated == null) {
            throw new BusinessException("退款申请失败，请稍后重试");
        }
        return toDto(updated);
    }

    @Transactional
    public TradeOrderDto approveRefund(Long actorUserId, Long orderId) {
        TradeOrderRecord order = requireOrderActorAndPendingConfirmation(actorUserId, orderId);
        if (!actorUserId.equals(order.sellerUserId())) {
            throw new BusinessException("仅卖家可处理退款");
        }
        if (!REFUND_STATUS_APPLIED.equals(order.refundStatus())) {
            throw new BusinessException("当前订单未处于退款申请状态");
        }

        int updatedRows;
        if (REFUND_TYPE_NO_RECEIPT.equals(order.refundType())) {
            updatedRows = tradeOrderRepository.approveRefundNoReceipt(orderId);
        } else if (REFUND_TYPE_RETURN_AFTER_RECEIPT.equals(order.refundType())) {
            updatedRows = tradeOrderRepository.approveRefundAfterReturn(orderId);
        } else {
            throw new BusinessException("退款类型无效");
        }
        if (updatedRows <= 0) {
            throw new BusinessException("退款处理失败，请稍后重试");
        }

        tradeOrderRepository.restoreOneStockOnRefund(order.itemId());
        TradeOrderRecord updated = tradeOrderRepository.findOrderById(orderId);
        if (updated == null) {
            throw new BusinessException("退款处理失败，请稍后重试");
        }
        return toDto(updated);
    }

    @Transactional
    public TradeOrderDto rejectRefund(Long actorUserId, Long orderId) {
        TradeOrderRecord order = requireOrderActorAndPendingConfirmation(actorUserId, orderId);
        if (!actorUserId.equals(order.sellerUserId())) {
            throw new BusinessException("仅卖家可处理退款");
        }
        if (!REFUND_STATUS_APPLIED.equals(order.refundStatus())) {
            throw new BusinessException("当前订单未处于退款申请状态");
        }

        int updatedRows = tradeOrderRepository.rejectRefund(orderId);
        if (updatedRows <= 0) {
            throw new BusinessException("退款处理失败，请稍后重试");
        }

        TradeOrderRecord updated = tradeOrderRepository.findOrderById(orderId);
        if (updated == null) {
            throw new BusinessException("退款处理失败，请稍后重试");
        }
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
                record.buyerConfirmed(),
                record.sellerConfirmed(),
                record.refundStatus(),
                record.refundType(),
                record.createdAt(),
                record.paidAt());
    }

    private TradeOrderRecord requireOrderActorAndPendingConfirmation(Long actorUserId, Long orderId) {
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
        if (!STATUS_PENDING_CONFIRMATION.equals(order.status())) {
            throw new BusinessException("当前订单状态不可操作");
        }
        return order;
    }

    private String normalizeRefundType(String refundType) {
        String normalized = refundType == null ? "" : refundType.trim().toUpperCase();
        if (REFUND_TYPE_NO_RECEIPT.equals(normalized) || REFUND_TYPE_RETURN_AFTER_RECEIPT.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException("退款类型无效");
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(ORDER_NO_DATE_FORMATTER);
        String prefix = ORDER_NO_PREFIX + datePart;
        String latestOrderNo = tradeOrderRepository.findLatestOrderNoByDatePrefix(prefix);

        int nextSequence = 1;
        if (latestOrderNo != null && latestOrderNo.length() >= prefix.length() + 6) {
            String tail = latestOrderNo.substring(prefix.length());
            try {
                nextSequence = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException ignored) {
                nextSequence = 1;
            }
        }
        if (nextSequence > 999999) {
            throw new BusinessException("当日订单量已达上限，请稍后重试");
        }
        return prefix + String.format("%06d", nextSequence);
    }

    private String mapOrderDetailStatusText(String status, String refundStatus) {
        if (REFUND_STATUS_REJECTED.equalsIgnoreCase(String.valueOf(refundStatus))) {
            return "待确认";
        }
        if (REFUND_STATUS_APPLIED.equalsIgnoreCase(String.valueOf(refundStatus))) {
            return "售后中";
        }
        if (STATUS_PENDING_PAYMENT.equals(status)) {
            return "待付款";
        }
        if (STATUS_PENDING_CONFIRMATION.equals(status)) {
            return "待确认";
        }
        if (STATUS_COMPLETED.equals(status)) {
            return "已完成";
        }
        if (STATUS_CANCELLED.equals(status)) {
            return "已取消";
        }
        return "进行中";
    }
}
