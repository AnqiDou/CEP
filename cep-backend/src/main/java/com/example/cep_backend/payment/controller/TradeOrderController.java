package com.example.cep_backend.payment.controller;

import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.service.AuthService;
import com.example.cep_backend.common.api.ApiResponse;
import com.example.cep_backend.payment.dto.CreateTradeOrderRequest;
import com.example.cep_backend.payment.dto.TradeOrderDto;
import com.example.cep_backend.payment.service.TradeOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment/orders")
public class TradeOrderController {
    private final TradeOrderService tradeOrderService;
    private final AuthService authService;

    public TradeOrderController(TradeOrderService tradeOrderService, AuthService authService) {
        this.tradeOrderService = tradeOrderService;
        this.authService = authService;
    }

    @PostMapping
    public ApiResponse<TradeOrderDto> createOrder(
            @RequestHeader("Authorization") String authorization,
            @RequestBody CreateTradeOrderRequest request) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("订单创建成功", tradeOrderService.createOrder(user.userId(), request));
    }

    @PostMapping("/{orderId}/pay-success")
    public ApiResponse<TradeOrderDto> paySuccess(@PathVariable Long orderId) {
        return ApiResponse.ok("支付成功", tradeOrderService.markOrderPaid(orderId));
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<TradeOrderDto> cancelOrder(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long orderId) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("取消成功", tradeOrderService.cancelOrder(user.userId(), orderId));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<TradeOrderDto> getOrder(@PathVariable Long orderId) {
        return ApiResponse.ok("获取成功", tradeOrderService.getOrder(orderId));
    }
}
