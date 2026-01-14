package ru.razumoff.minio;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.razumoff.commonlib.exceptions.ErrorCode;
import ru.razumoff.commonlib.exceptions.PlatformException;
import ru.razumoff.config.MinioConfig;

import java.net.URI;
import java.util.UUID;

import static ru.razumoff.Constants.Minio.PUBLIC_READ_POLICY_TEMPLATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileService implements IMinioFileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Value("${minio.bucket-name}")
    private String bucketName;

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

    @Override
    public void deleteImage(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl);
            String path = uri.getPath().substring(1);
            String[] parts = path.split("/", 2);
            String bucket = parts[0];
            String object = parts[1];

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(object)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to delete image from bucket {}", bucketName, e);
            throw new PlatformException(ErrorCode.FAILED_DELETE_IMAGE);
        }
    }

    private String uploadImage(MultipartFile imageFile, String bucketName) throws Exception {
        String fileName = UUID.randomUUID() + "." + extractExtension(imageFile.getOriginalFilename());
        createBucketWithPolicy(bucketName);

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName).object(fileName)
                .stream(imageFile.getInputStream(), imageFile.getSize(), -1)
                .contentType(imageFile.getContentType())
                .build());

        log.info("Success uploaded image {} to bucket {}", fileName, bucketName);
        return minioConfig.getPublicUrl() + bucketName + "/" + fileName;
    }

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

    private void validateImage(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024 || !file.getContentType().startsWith("image/")) {
            log.error("Invalid image file: {}", file.getOriginalFilename());
            throw new PlatformException(ErrorCode.INVALID_IMAGE);
        }
    }

    private String extractExtension(String filename) {
        return filename != null ? filename.substring(filename.lastIndexOf('.') + 1) : "png";
    }
}
