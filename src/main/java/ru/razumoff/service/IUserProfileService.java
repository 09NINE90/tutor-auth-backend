package ru.razumoff.service;


import ru.razumoff.dao.dto.request.EditUserProfileRqDto;
import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.entity.UserProfileEntity;

import java.util.List;
import java.util.UUID;

public interface IUserProfileService {
    UserProfileEntity getProfileByUserId(UUID uuid);

    void updateProfileAvatar(UUID uuid, String s3Key);

    void updateUserProfileData(UUID id, EditUserProfileRqDto request);

    void addUserProfile(UUID id, RegisterRequest request);

    List<UserProfileEntity> getProfilesByUserIds(List<UUID> userIds);
}
