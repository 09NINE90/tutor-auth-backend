package ru.razumoff.service;


import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.entity.UserProfileEntity;

import java.util.UUID;

public interface IUserProfileService {
    UserProfileEntity getProfileByUserId(UUID uuid);

    void addUserProfile(UUID id, RegisterRequest request);
}
