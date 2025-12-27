package ru.razumoff.service;

import ru.razumoff.dao.dto.response.UserProfileResponse;

import java.util.UUID;

public interface IUserService {
    UserProfileResponse getUserProfileById(UUID uuid);
}
