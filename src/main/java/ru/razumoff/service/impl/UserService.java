package ru.razumoff.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.razumoff.commonlib.exceptions.ErrorCode;
import ru.razumoff.commonlib.exceptions.PlatformException;
import ru.razumoff.dao.dto.response.UserProfileResponse;
import ru.razumoff.dao.entity.UserEntity;
import ru.razumoff.dao.entity.UserProfileEntity;
import ru.razumoff.dao.repository.UserRepository;
import ru.razumoff.service.IUserProfileService;
import ru.razumoff.service.IUserService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final IUserProfileService userProfileService;

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
}
