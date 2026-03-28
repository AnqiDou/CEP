package com.example.cep_backend.payment.controller;

import com.example.cep_backend.common.api.ApiResponse;
import com.example.cep_backend.payment.dto.CreateTradeOrderRequest;
import com.example.cep_backend.payment.dto.TradeOrderDto;
import com.example.cep_backend.payment.service.TradeOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment/orders")
public class TradeOrderController {
    private final TradeOrderService tradeOrderService;

    public TradeOrderController(TradeOrderService tradeOrderService) {
        this.tradeOrderService = tradeOrderService;
    }

    @PostMapping
    public ApiResponse<TradeOrderDto> createOrder(@RequestBody CreateTradeOrderRequest request) {
        return ApiResponse.ok("订单创建成功", tradeOrderService.createOrder(request));
    }

    @PostMapping("/{orderId}/pay-success")
    public ApiResponse<TradeOrderDto> paySuccess(@PathVariable Long orderId) {
        return ApiResponse.ok("支付成功", tradeOrderService.markOrderPaid(orderId));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<TradeOrderDto> getOrder(@PathVariable Long orderId) {
        return ApiResponse.ok("获取成功", tradeOrderService.getOrder(orderId));
    }
}
