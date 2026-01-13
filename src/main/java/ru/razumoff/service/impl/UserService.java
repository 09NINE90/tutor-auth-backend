package ru.razumoff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.razumoff.commonlib.exceptions.ErrorCode;
import ru.razumoff.commonlib.exceptions.PlatformException;
import ru.razumoff.dao.dto.response.AvatarResponse;
import ru.razumoff.dao.dto.response.UserProfileResponse;
import ru.razumoff.dao.entity.UserEntity;
import ru.razumoff.dao.entity.UserProfileEntity;
import ru.razumoff.dao.repository.UserRepository;
import ru.razumoff.minio.IMinioFileService;
import ru.razumoff.service.IUserProfileService;
import ru.razumoff.service.IUserService;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final IUserProfileService userProfileService;
    private final IMinioFileService minIOFileService;

    @Override
    public UserProfileResponse getUserProfileById(UUID uuid) {
        UserEntity entity = userRepository.findById(uuid).orElseThrow(
                () -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND)
        );

        UserProfileEntity userProfile = userProfileService.getProfileByUserId(uuid);

        return UserProfileResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .roles(entity.getRoles())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .middleName(userProfile.getMiddleName())
                .birthDate(userProfile.getBirthDate())
                .gender(userProfile.getGender().getRu_name())
                .avatarUrl(userProfile.getAvatarUrl())
                .build();
    }

    @Override
    @Transactional
    public AvatarResponse uploadAvatar(UUID uuid, MultipartFile avatarFile) {
        UserEntity entity = userRepository.findById(uuid).orElseThrow(
                () -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND)
        );

        UserProfileEntity userProfile = userProfileService.getProfileByUserId(uuid);

        String oldAvatarUrl = userProfile.getAvatarUrl();
        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            try {
                minIOFileService.deleteImage(oldAvatarUrl);
            } catch (Exception e) {
                log.warn("Failed to delete old avatar {}: {}", oldAvatarUrl, e.getMessage());
            }
        }

        String imageUrl = minIOFileService.uploadAvatarImage(avatarFile);

        userProfileService.updateProfileAvatar(entity.getId(), imageUrl);

        return new AvatarResponse(imageUrl);
    }
}
