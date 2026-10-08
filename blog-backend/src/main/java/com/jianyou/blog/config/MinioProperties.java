package com.jianyou.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 对象存储配置（对应 application.yml 的 minio.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /** 服务地址，如 http://localhost:9000 */
    private String endpoint;

    private String accessKey;

    private String secretKey;

    /** 存储桶名 */
    private String bucket;

    /** 浏览器访问地址前缀（与 endpoint 可不同，生产走 nginx 反代） */
    private String accessUrl;
}
