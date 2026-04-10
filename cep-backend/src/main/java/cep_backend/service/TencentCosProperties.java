package cep_backend.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TencentCosProperties {
    private final String secretId;
    private final String secretKey;
    private final String region;
    private final String bucketName;
    private final String baseUrl;
    private final String pathPrefix;

    public TencentCosProperties(
            @Value("${app.tencent.cos.secret-id:}") String secretId,
            @Value("${app.tencent.cos.secret-key:}") String secretKey,
            @Value("${app.tencent.cos.region:}") String region,
            @Value("${app.tencent.cos.bucket-name:}") String bucketName,
            @Value("${app.tencent.cos.base-url:}") String baseUrl,
            @Value("${app.tencent.cos.path-prefix:publish-images}") String pathPrefix) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.region = region;
        this.bucketName = bucketName;
        this.baseUrl = baseUrl;
        this.pathPrefix = pathPrefix;
    }

    public String secretId() {
        return secretId;
    }

    public String secretKey() {
        return secretKey;
    }

    public String region() {
        return region;
    }

    public String bucketName() {
        return bucketName;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String pathPrefix() {
        return pathPrefix;
    }
}
