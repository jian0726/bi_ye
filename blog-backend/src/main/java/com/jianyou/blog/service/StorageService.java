package com.jianyou.blog.service;

import com.jianyou.blog.common.BusinessException;
import com.jianyou.blog.config.MinioProperties;
import com.jianyou.blog.entity.UploadFile;
import com.jianyou.blog.mapper.UploadFileMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 对象存储服务：统一管理文件上传（MinIO）
 * 管理端素材（图片/音频）与用户头像共用同一条上传链路，均记录入库 t_file
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final UploadFileMapper uploadFileMapper;

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private static final Set<String> AUDIO_TYPES = Set.of(
            "audio/mpeg", "audio/mp3", "audio/flac", "audio/x-flac");

    /** flac 浏览器常报 octet-stream，故同时按后缀识别 */
    private static final Set<String> AUDIO_EXTS = Set.of("mp3", "flac");

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final long MAX_AUDIO_SIZE = 1028L * 1024 * 1024;

    private static final Map<String, String> TYPE_EXT = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp",
            "audio/mpeg", ".mp3",
            "audio/mp3", ".mp3",
            "audio/flac", ".flac",
            "audio/x-flac", ".flac");

    /** 管理端素材上传：图片（≤5MB）或音频（≤1028MB） */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        String contentType = lowerType(file);
        String ext = rawExt(file);

        boolean isAudio;
        String storeType;
        String fileExt;
        if (IMAGE_TYPES.contains(contentType)) {
            isAudio = false;
            storeType = contentType;
            fileExt = TYPE_EXT.get(contentType);
        } else if (AUDIO_TYPES.contains(contentType) || AUDIO_EXTS.contains(ext)) {
            isAudio = true;
            // 统一存标准 MIME：保证前台 <audio>/<img> 与浏览器解码一致
            fileExt = "flac".equals(ext) ? ".flac" : ".mp3";
            storeType = ".flac".equals(fileExt) ? "audio/flac" : "audio/mpeg";
        } else {
            throw new BusinessException(400, "仅支持 jpg / png / gif / webp 图片，或 mp3 / flac 音频");
        }

        long maxSize = isAudio ? MAX_AUDIO_SIZE : MAX_IMAGE_SIZE;
        if (file.getSize() > maxSize) {
            throw new BusinessException(400, isAudio ? "音频大小不能超过 1028MB" : "图片大小不能超过 5MB");
        }
        return doStore(file, storeType, fileExt);
    }

    /** 用户头像上传：仅图片，≤2MB */
    public String storeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的图片");
        }
        String contentType = lowerType(file);
        if (!IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(400, "头像仅支持 jpg / png / gif / webp 格式");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(400, "头像大小不能超过 2MB");
        }
        return doStore(file, contentType, TYPE_EXT.get(contentType));
    }

    /** 通用图片上传（如留言页自定义背景）：仅图片，≤5MB */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的图片");
        }
        String contentType = lowerType(file);
        if (!IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(400, "仅支持 jpg / png / gif / webp 图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(400, "图片大小不能超过 5MB");
        }
        return doStore(file, contentType, TYPE_EXT.get(contentType));
    }

    private String doStore(MultipartFile file, String storeType, String fileExt) {
        ensureBucket();

        // 按日期分目录 + UUID 文件名，避免重名覆盖
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectKey = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + fileExt;

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(storeType)
                    .build());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(500, "文件上传失败，请确认 MinIO 服务已启动");
        }

        String url = properties.getAccessUrl() + "/" + properties.getBucket() + "/" + objectKey;

        // 记录入库，供资源管理页展示与删除
        UploadFile record = new UploadFile();
        record.setName(file.getOriginalFilename() != null ? file.getOriginalFilename() : objectKey);
        record.setObjectKey(objectKey);
        record.setUrl(url);
        record.setType(storeType);
        record.setSize(file.getSize());
        record.setCreateTime(LocalDateTime.now());
        uploadFileMapper.insert(record);

        return url;
    }

    /** 桶不存在则创建，并设置为匿名只读（前台 <img> 可直接访问） */
    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(properties.getBucket()).build());
                String policy = """
                        {
                          "Version": "2012-10-17",
                          "Statement": [{
                            "Effect": "Allow",
                            "Principal": {"AWS": ["*"]},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                          }]
                        }""".formatted(properties.getBucket());
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(properties.getBucket()).config(policy).build());
            }
        } catch (Exception e) {
            log.error("MinIO 桶初始化失败", e);
            throw new BusinessException(500, "存储服务不可用，请确认 MinIO 已启动");
        }
    }

    private String lowerType(MultipartFile file) {
        return file.getContentType() == null ? "" : file.getContentType().toLowerCase();
    }

    private String rawExt(MultipartFile file) {
        String rawName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        return rawName.contains(".")
                ? rawName.substring(rawName.lastIndexOf('.') + 1).toLowerCase()
                : "";
    }
}
