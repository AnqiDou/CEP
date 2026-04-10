package cep_backend.controller;
import cep_backend.dto.AuthUserDto;
import cep_backend.service.AuthService;
import cep_backend.common.result.ApiResponse;
import cep_backend.dto.PublishImageUploadDto;
import cep_backend.dto.PublishItemDto;
import cep_backend.dto.PublishItemRequest;
import cep_backend.dto.PublishItemStatusRequest;
import cep_backend.dto.PublishItemUpdateRequest;
import cep_backend.dto.PublishOwnedItemDto;
import cep_backend.service.PublishImageService;
import cep_backend.service.PublishService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

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

    @GetMapping("/items/mine")
    public ApiResponse<List<PublishOwnedItemDto>> getMyItems(
            @RequestHeader("Authorization") String authorization) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("获取成功", publishService.getMyItems(user.userId()));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<PublishOwnedItemDto> updateMyItem(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long itemId,
            @RequestBody PublishItemUpdateRequest request) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("更新成功", publishService.updateMyItem(user.userId(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> deleteMyItem(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long itemId) {
        AuthUserDto user = authService.currentUser(authorization);
        publishService.deleteMyItem(user.userId(), itemId);
        return ApiResponse.ok("删除成功");
    }

    @PatchMapping("/items/{itemId}/status")
    public ApiResponse<PublishOwnedItemDto> updateMyItemStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long itemId,
            @RequestBody PublishItemStatusRequest request) {
        AuthUserDto user = authService.currentUser(authorization);
        return ApiResponse.ok("更新成功", publishService.updateMyItemStatus(user.userId(), itemId, request.status()));
    }
}
