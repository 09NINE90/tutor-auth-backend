package ru.razumoff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.razumoff.dto.integration.ProfileRsDto;
import ru.razumoff.exceptions.ErrorCode;
import ru.razumoff.exceptions.PlatformException;
import ru.razumoff.dao.dto.response.AvatarResponse;
import ru.razumoff.dao.dto.response.UserProfileResponse;
import ru.razumoff.dao.entity.UserEntity;
import ru.razumoff.dao.entity.UserProfileEntity;
import ru.razumoff.dao.repository.UserRepository;
import ru.razumoff.minio.IMinioFileService;
import ru.razumoff.service.IUserProfileService;
import ru.razumoff.service.IUserService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final IUserProfileService userProfileService;
    private final IMinioFileService minIOFileService;

    /**
     * Получить полный профиль пользователя (User + Profile + Avatar URL)
     */
    @Override
    public UserProfileResponse getUserProfileById(UUID uuid) {
        UserEntity entity = userRepository.findById(uuid).orElseThrow(
                () -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND)
        );

        UserProfileEntity userProfile = userProfileService.getProfileByUserId(uuid);

        return UserProfileResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .middleName(userProfile.getMiddleName())
                .birthDate(userProfile.getBirthDate())
                .gender(userProfile.getGender().getRu_name())
                .avatarUrl(minIOFileService.generatePublicUrl(userProfile.getAvatarS3Key()))
                .build();
    }

    /**
     * Загрузить аватарку: удалить старую + сохранить новую в MinIO
     */
    @Override
    @Transactional
    public AvatarResponse uploadAvatar(UUID uuid, MultipartFile avatarFile) {
        UserEntity entity = userRepository.findById(uuid).orElseThrow(
                () -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND)
        );

        UserProfileEntity userProfile = userProfileService.getProfileByUserId(uuid);

        String oldAvatarUrl = userProfile.getAvatarS3Key();
        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            try {
                minIOFileService.deleteImage(oldAvatarUrl);
            } catch (Exception e) {
                log.warn("Failed to delete old avatar {}: {}", oldAvatarUrl, e.getMessage());
            }
        }

        String s3Key = minIOFileService.uploadAvatarImage(avatarFile);

        userProfileService.updateProfileAvatar(entity.getId(), s3Key);

        return new AvatarResponse(minIOFileService.generatePublicUrl(s3Key));
    }

    /**
     * Получить профили по списку userId (User+Profile+Avatar)
     */
    @Override
    public List<ProfileRsDto> getUserProfilesByIds(List<UUID> userIds) {
        List<UserEntity> users = userRepository.findAllById(userIds);
        List<UserProfileEntity> profiles = userProfileService.getProfilesByUserIds(userIds);

        if (users.isEmpty() || profiles.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, UserProfileEntity> profileMap = profiles.stream()
                .collect(Collectors.toMap(
                        UserProfileEntity::getUserId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        return users.stream()
                .filter(user -> user.getId() != null)
                .map(user -> {
                    UserProfileEntity profile = profileMap.get(user.getId());
                    return new ProfileRsDto(
                            user.getId(),
                            user.getEmail(),
                            profile != null ? profile.getFirstName() : null,
                            profile != null ? profile.getLastName() : null,
                            profile != null ? minIOFileService.generatePublicUrl(profile.getAvatarS3Key()) : null
                    );
                })
                .toList();
    }
}
