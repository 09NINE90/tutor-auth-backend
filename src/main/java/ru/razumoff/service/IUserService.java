package ru.razumoff.service;

import org.springframework.web.multipart.MultipartFile;
import ru.razumoff.dao.dto.response.AvatarResponse;
import ru.razumoff.dao.dto.response.UserProfileResponse;

import java.util.UUID;

public interface IUserService {
    UserProfileResponse getUserProfileById(UUID uuid);

    AvatarResponse uploadAvatar(UUID uuid, MultipartFile avatarFile);
}
