package com.example.cep_backend.itemdetail.controller;

import com.example.cep_backend.common.api.ApiResponse;
import com.example.cep_backend.itemdetail.dto.ItemDetailDto;
import com.example.cep_backend.itemdetail.service.ItemDetailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class ItemDetailController {
    private final ItemDetailService itemDetailService;

    public ItemDetailController(ItemDetailService itemDetailService) {
        this.itemDetailService = itemDetailService;
    }

    @GetMapping("/{itemId}/detail")
    public ApiResponse<ItemDetailDto> detail(@PathVariable Long itemId) {
        return ApiResponse.ok("获取成功", itemDetailService.getItemDetail(itemId));
    }
}
