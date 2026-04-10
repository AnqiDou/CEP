package cep_backend.service;
import cep_backend.common.exception.BusinessException;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
public class PublishImageService {
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif");

    private final TencentCosProperties properties;

    public PublishImageService(TencentCosProperties properties) {
        this.properties = properties;
    }

    public String upload(MultipartFile file) {
        validateCosConfig();
        validateImage(file);

        String key = buildObjectKey(file.getOriginalFilename());
        COSCredentials credentials = new BasicCOSCredentials(properties.secretId(), properties.secretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(properties.region()));
        clientConfig.setHttpProtocol(HttpProtocol.https);

        COSClient cosClient = new COSClient(credentials, clientConfig);
        try (InputStream stream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest request = new PutObjectRequest(properties.bucketName(), key, stream, metadata);
            cosClient.putObject(request);
            return buildUrl(key);
        } catch (IOException ex) {
            throw new BusinessException("上传图片失败，请稍后重试");
        } finally {
            cosClient.shutdown();
        }
    }

    private void validateCosConfig() {
        if (isBlank(properties.secretId()) || isBlank(properties.secretKey()) || isBlank(properties.region())
                || isBlank(properties.bucketName())) {
            throw new BusinessException("腾讯云 COS 配置不完整，请联系管理员");
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传图片文件");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("单张图片大小不能超过 10MB");
        }
        String contentType = file.getContentType();
        if (isBlank(contentType) || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("仅支持 JPG、PNG、WEBP、GIF 图片");
        }
    }

    private String buildObjectKey(String originalFilename) {
        String extension = resolveExtension(originalFilename);
        String dateFolder = LocalDate.now().toString();
        String prefix = normalizePathPrefix(properties.pathPrefix());
        return prefix + "/" + dateFolder + "/" + UUID.randomUUID() + extension;
    }

    private String resolveExtension(String filename) {
        if (isBlank(filename)) {
            return ".jpg";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return ".jpg";
        }
        String extension = filename.substring(index).toLowerCase();
        if (extension.length() > 8) {
            return ".jpg";
        }
        return extension;
    }

    private String buildUrl(String key) {
        if (!isBlank(properties.baseUrl())) {
            String base = properties.baseUrl().endsWith("/")
                    ? properties.baseUrl().substring(0, properties.baseUrl().length() - 1)
                    : properties.baseUrl();
            return base + "/" + key;
        }
        return "https://" + properties.bucketName() + ".cos." + properties.region() + ".myqcloud.com/" + key;
    }

    private String normalizePathPrefix(String value) {
        if (isBlank(value)) {
            return "publish-images";
        }
        String normalized = value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? "publish-images" : normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
