package ru.razumoff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.razumoff.exceptions.ErrorCode;
import ru.razumoff.exceptions.PlatformException;
import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.entity.UserProfileEntity;
import ru.razumoff.dao.repository.UserProfileRepository;
import ru.razumoff.service.IUserProfileService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService implements IUserProfileService {

    private final UserProfileRepository repository;


    /**
     * Получить профиль пользователя по userId
     */
    @Override
    public UserProfileEntity getProfileByUserId(UUID uuid) {
        return repository.findByUserId(uuid).orElseThrow(
                () -> new PlatformException(ErrorCode.AUTH_USER_PROFILE_NOT_FOUND)
        );
    }

    /**
     * Обновить S3 ключ аватарки пользователя
     */
    @Override
    public void updateProfileAvatar(UUID uuid, String s3Key) {
        UserProfileEntity entity = repository.findByUserId(uuid).orElseThrow(
                () -> new PlatformException(ErrorCode.AUTH_USER_PROFILE_NOT_FOUND)
        );

        entity.setAvatarS3Key(s3Key);
        entity.setUpdatedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    /**
     * Создать профиль при регистрации
     */
    @Override
    public void addUserProfile(UUID id, RegisterRequest request) {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(id);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setMiddleName(request.getMiddleName());
        profile.setGender(request.getGender());
        profile.setBirthDate(request.getBirthDate());
        profile.setCreatedAt(OffsetDateTime.now());

        repository.save(profile);
    }

    /**
     * Получить профили по списку userId
     */
    @Override
    public List<UserProfileEntity> getProfilesByUserIds(List<UUID> userIds) {
        return repository.findByUserIds(userIds);
    }
}
