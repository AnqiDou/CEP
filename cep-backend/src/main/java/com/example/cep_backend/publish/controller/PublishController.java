package com.example.cep_backend.publish.controller;

import com.example.cep_backend.auth.dto.AuthUserDto;
import com.example.cep_backend.auth.service.AuthService;
import com.example.cep_backend.common.api.ApiResponse;
import com.example.cep_backend.publish.dto.PublishImageUploadDto;
import com.example.cep_backend.publish.dto.PublishItemDto;
import com.example.cep_backend.publish.dto.PublishItemRequest;
import com.example.cep_backend.publish.service.PublishImageService;
import com.example.cep_backend.publish.service.PublishService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/publish")
public class PublishController {
    private final PublishService publishService;
    private final PublishImageService publishImageService;
    private final AuthService authService;

    public PublishController(PublishService publishService,
            PublishImageService publishImageService,
            AuthService authService) {
        this.publishService = publishService;
        this.publishImageService = publishImageService;
        this.authService = authService;
    }

    @PostMapping("/items")
    public ApiResponse<PublishItemDto> publishItem(
            @RequestHeader("Authorization") String authorization,
            @RequestBody PublishItemRequest request) {
        AuthUserDto user = authService.currentUser(authorization);
        PublishItemDto item = publishService.publishItem(user.userId(), request);
        return ApiResponse.ok("发布成功", item);
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PublishImageUploadDto> uploadImage(
            @RequestHeader("Authorization") String authorization,
            @RequestPart("file") MultipartFile file) {
        authService.currentUser(authorization);
        String url = publishImageService.upload(file);
        return ApiResponse.ok("上传成功", new PublishImageUploadDto(url));
    }
}
