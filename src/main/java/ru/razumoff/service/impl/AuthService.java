package ru.razumoff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.razumoff.config.security.JwtService;
import ru.razumoff.dao.entity.PermissionEntity;
import ru.razumoff.exceptions.ErrorCode;
import ru.razumoff.exceptions.PlatformException;
import ru.razumoff.dao.dto.request.LoginRequest;
import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.dto.response.TokenResponse;
import ru.razumoff.dao.entity.RoleEntity;
import ru.razumoff.dao.entity.UserEntity;
import ru.razumoff.dao.repository.RoleRepository;
import ru.razumoff.dao.repository.UserRepository;
import ru.razumoff.service.IAuthService;
import ru.razumoff.service.IUserProfileService;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final IUserProfileService userProfileService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;

    /**
     * Регистрация нового пользователя STUDENT
     * Создает UserEntity + Profile + JWT токены
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail().trim()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        try {
            UserEntity user = new UserEntity();
            user.setEmail(request.getEmail().trim());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setEnabled(true);
            user.setCreatedAt(OffsetDateTime.now());

            RoleEntity roleUser = getRoleEntity(request.isTutor());

            user.setRole(roleUser);

            UserEntity savedUser = userRepository.save(user);
            userProfileService.addUserProfile(user.getId(), request);

            Set<String> permissionsNames = getPermissionsNames(savedUser);

            String access = jwtService.generateAccessToken(
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getRole().getName(),
                    permissionsNames
            );
            String refresh = jwtService.generateRefreshToken(
                    savedUser.getId(),
                    savedUser.getEmail().trim(),
                    savedUser.getRole().getName(),
                    permissionsNames
            );

            log.info("User registered success: {}", user);
            return new TokenResponse(access, refresh, null);
        } catch (Exception e) {
            log.error("Registration failed for {}: {}", request.getEmail(), e.getMessage());
            throw new PlatformException(ErrorCode.AUTH_REGISTRATION_FAILED);
        }
    }

    /**
     * Установка роли пользователя в зависимости от вхожного параметра
     */
    private RoleEntity getRoleEntity(boolean isTutor) {
        RoleEntity roleUser;
        if (isTutor) {
            roleUser = roleRepository.findByName("TUTOR_LIMITED").orElseThrow(
                    () -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND)
            );
        } else {
            roleUser = roleRepository.findByName("STUDENT_FULL").orElseThrow(
                    () -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND)
            );
        }
        return roleUser;
    }

    /**
     * Авторизация пользователя по email/password
     * Возвращает JWT access+refresh токены
     */
    public TokenResponse login(LoginRequest request) {
        try {
            var authToken = new UsernamePasswordAuthenticationToken(
                    request.getEmail(), request.getPassword()
            );
            authenticationManager.authenticate(authToken);

            UserEntity user = getUserWithRolesAndPermissionsOrThrow(request.getEmail());

            String roleName = user.getRole().getName();
            Set<String> permissionsNames = getPermissionsNames(user);

            String access = jwtService.generateAccessToken(
                    user.getId(),
                    user.getEmail(),
                    roleName,
                    permissionsNames
            );
            String refresh = jwtService.generateRefreshToken(
                    user.getId(),
                    user.getEmail(),
                    roleName,
                    permissionsNames
            );

            log.info("User login success: {}", user);
            return new TokenResponse(access, refresh, null);
        } catch (AuthenticationException ex) {
            log.warn("Login failed for {}: invalid credentials", request.getEmail());
            throw new PlatformException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        } catch (Exception e) {
            log.error("Login failed for {}: {}", request.getEmail(), e.getMessage());
            throw new PlatformException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Обновление access токена по refresh токену
     * Выдает новую пару access+refresh токенов
     */
    public TokenResponse refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new PlatformException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND)
        );

        UserEntity userWithRoles = getUserWithRolesAndPermissionsOrThrow(user.getEmail());

        String roleName = userWithRoles.getRole().getName();
        Set<String> permissionsNames = getPermissionsNames(userWithRoles);

        String newAccess = jwtService.generateAccessToken(
                userWithRoles.getId(),
                userWithRoles.getEmail(),
                roleName,
                permissionsNames
        );
        String newRefresh = jwtService.generateRefreshToken(
                userWithRoles.getId(),
                userWithRoles.getEmail(),
                roleName,
                permissionsNames
        );

        log.info("Token refreshed success. userId {}", userId);
        return new TokenResponse(newAccess, newRefresh, null);
    }

    /**
     * Преобразование пермишенов пользователя в список названий
     */
    private Set<String> getPermissionsNames(UserEntity user) {
        return user.getRole().getPermissions()
                .stream()
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Получть пользователя со списком его ролей по email
     */
    private UserEntity getUserWithRolesAndPermissionsOrThrow(String userEmail) {
        return userRepository.findByEmailWithRolesAndPermissions(userEmail).orElseThrow(
                () -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND)
        );
    }
}
