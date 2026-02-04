package ru.razumoff.minio;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.razumoff.commonlib.exceptions.ErrorCode;
import ru.razumoff.commonlib.exceptions.PlatformException;

import java.util.UUID;

import static ru.razumoff.Constants.Minio.PUBLIC_READ_POLICY_TEMPLATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileService implements IMinioFileService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Загрузка аватарки в MinIO (валидация + UUID имя)
     */
    @Override
    public String uploadAvatarImage(MultipartFile imageFile) {
        validateImage(imageFile);
        try {
            return uploadImage(imageFile, bucketName);
        } catch (Exception e) {
            log.error("Failed to upload image to bucket {}", bucketName, e);
            throw new PlatformException(ErrorCode.FAILED_UPLOAD_IMAGE);
        }
    }

    /**
     * Удаление файла из MinIO по S3 ключу
     */
    @Override
    public void deleteImage(String s3Key) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(s3Key)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to delete image from bucket {}, file: {}", bucketName, s3Key, e);
            throw new PlatformException(ErrorCode.FAILED_DELETE_IMAGE);
        }
    }

    /**
     * Генерация публичной presigned URL (7 дней)
     */
    @Override
    public String generatePublicUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) return "";
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(s3Key)
                            .expiry(7 * 24 * 60 * 60)
                            .build()
            );
        } catch (Exception e) {
            log.error("Presigned failed for {}", s3Key, e);
            throw new RuntimeException("S3 access error", e);
        }
    }

    /**
     * Загрузка изображения в bucket (создание bucket если нет)
     */
    private String uploadImage(MultipartFile imageFile, String bucketName) throws Exception {
        String fileName = UUID.randomUUID() + "." + extractExtension(imageFile.getOriginalFilename());
        createBucketWithPolicy(bucketName);

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName).object(fileName)
                .stream(imageFile.getInputStream(), imageFile.getSize(), -1)
                .contentType(imageFile.getContentType())
                .build());

        log.info("Success uploaded file {} to bucket {}", fileName, bucketName);
        return fileName;
    }

    /**
     * Создание bucket с public-read политикой
     */
    public void createBucketWithPolicy(String bucketName) throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());

        if (bucketExists) {
            return;
        }

        MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder()
                .bucket(bucketName).build();
        minioClient.makeBucket(makeBucketArgs);

        String policyJson = PUBLIC_READ_POLICY_TEMPLATE.formatted(bucketName);

        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(bucketName)
                .config(policyJson)
                .build());
    }

    /**
     * Валидация изображения (размер <10MB, image/*)
     */
    private void validateImage(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024 || !file.getContentType().startsWith("image/")) {
            log.error("Invalid image file: {}", file.getOriginalFilename());
            throw new PlatformException(ErrorCode.INVALID_IMAGE);
        }
    }

    /**
     * Извлечение расширения из filename
     */
    private String extractExtension(String filename) {
        return filename != null ? filename.substring(filename.lastIndexOf('.') + 1) : "png";
    }
}
