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
import ru.razumoff.dao.dto.internal.TokensAndPermissionsDto;
import ru.razumoff.dao.entity.*;
import ru.razumoff.exceptions.ErrorCode;
import ru.razumoff.exceptions.PlatformException;
import ru.razumoff.dao.dto.request.LoginRequest;
import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.dto.response.AuthRsDto;
import ru.razumoff.dao.repository.RoleRepository;
import ru.razumoff.dao.repository.UserRepository;
import ru.razumoff.service.IAuthService;
import ru.razumoff.service.ICookieService;
import ru.razumoff.service.IUserProfileService;
import ru.razumoff.utils.DtoUtils;

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
    private final ICookieService cookieService;

    /**
     * Регистрация нового пользователя STUDENT
     * Создает UserEntity + Profile + JWT токены
     */
    @Transactional
    public AuthRsDto register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail();
        boolean isValidEmail = email != null && !email.isEmpty();

        try {
            if (userRepository.findByUsername(username).isPresent()) {
                throw new PlatformException(ErrorCode.AUTH_USER_EXISTS, String.format("Логин \"%s\" занят", username));
            }

            if (isValidEmail && userRepository.findByEmail(email).isPresent()) {
                throw new PlatformException(ErrorCode.AUTH_USER_EXISTS, String.format("Email \"%s\" занят", email));
            }

            UserEntity user = new UserEntity();
            user.setUsername(username);
            user.setEmail(isValidEmail ? email : null);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setEnabled(true);

            RoleEntity roleUser = getRoleEntity(request.isTutor());

            user.setRole(roleUser);

            UserEntity savedUser = userRepository.save(user);
            userProfileService.addUserProfile(user.getId(), request);

            TokensAndPermissionsDto tokens = getTokensAndPermissions(savedUser);

            cookieService.addRefreshCookie(tokens.getRefreshToken());

            log.info("User registered success: {}", user.getId());
            return new AuthRsDto(tokens.getAccessToken(), tokens.getPermissions());
        } catch (PlatformException e) {
            throw e;
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
                    () -> new PlatformException(ErrorCode.ROLE_NOT_FOUND)
            );
        } else {
            roleUser = roleRepository.findByName("STUDENT_FULL").orElseThrow(
                    () -> new PlatformException(ErrorCode.ROLE_NOT_FOUND)
            );
        }
        return roleUser;
    }

    /**
     * Авторизация пользователя по email/password
     * Возвращает JWT access+refresh токены
     */
    public AuthRsDto login(LoginRequest request) {
        String login = request.getLogin().trim();

        try {
            UserEntity user = getUserWithRolesAndPermissionsOrThrow(login);

            var authToken = new UsernamePasswordAuthenticationToken(
                    login, request.getPassword()
            );
            authenticationManager.authenticate(authToken);

            TokensAndPermissionsDto tokens = getTokensAndPermissions(user);

            cookieService.addRefreshCookie(tokens.getRefreshToken());

            log.info("User login success: {}", user.getId());
            return new AuthRsDto(tokens.getAccessToken(), tokens.getPermissions());
        } catch (AuthenticationException ex) {
            log.warn("Login failed for {}: invalid credentials", request.getLogin());
            throw new PlatformException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        } catch (Exception e) {
            log.error("Login failed for {}: {}", request.getLogin(), e.getMessage());
            throw new PlatformException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Обновление access токена по refresh токену
     * Выдает новую пару access+refresh токенов
     */
    public AuthRsDto refresh(String refreshToken) {
        try {
            if (!jwtService.isTokenValid(refreshToken)) {
                cookieService.deleteRefreshCookie();
                throw new PlatformException(ErrorCode.AUTH_REFRESH_INVALID);
            }

            UUID userId = jwtService.extractUserId(refreshToken);
            UserEntity user = userRepository.findByUserIdWithRolesAndPermissions(userId).orElseThrow(() -> {
                cookieService.deleteRefreshCookie();
                return new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND);
            });

            TokensAndPermissionsDto tokens = getTokensAndPermissions(user);

            cookieService.addRefreshCookie(tokens.getRefreshToken());

            log.info("Token refreshed success. userId {}", userId);
            return new AuthRsDto(tokens.getAccessToken(), tokens.getPermissions());
        } catch (PlatformException e) {
            cookieService.deleteRefreshCookie();
            throw e;
        } catch (Exception e) {
            cookieService.deleteRefreshCookie();
            log.error("Token refresh failed: {}", e.getMessage());
            throw new PlatformException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        try {
            cookieService.deleteRefreshCookie();
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
        }
    }

    /**
     * Генерирует пару JWT токенов (access + refresh) и список разрешений пользователя.
     *
     * @param user пользователь с загруженными ролью и разрешениями (через FETCH JOIN)
     * @return DTO с access токеном, refresh токеном и списком разрешений
     */
    private TokensAndPermissionsDto getTokensAndPermissions(UserEntity user) {
        String roleName = DtoUtils.safelyGet(user, UserEntity::getRole, RoleEntity::getName);
        Set<String> permissionsNames = getPermissionsNames(user);

        String access = jwtService.generateAccessToken(
                DtoUtils.safelyGet(user, UserEntity::getId),
                DtoUtils.safelyGet(user, UserEntity::getUsername),
                roleName,
                permissionsNames
        );
        String refresh = jwtService.generateRefreshToken(
                DtoUtils.safelyGet(user, UserEntity::getId),
                DtoUtils.safelyGet(user, UserEntity::getUsername),
                roleName,
                permissionsNames
        );

        return TokensAndPermissionsDto.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .permissions(permissionsNames)
                .build();
    }

    /**
     * Извлекает названия разрешений из роли пользователя.
     *
     * @param user пользователь с загруженными разрешениями
     * @return множество строковых названий разрешений, пустое множество если разрешений нет
     */
    private Set<String> getPermissionsNames(UserEntity user) {
        return DtoUtils.safelyGetSet(user, UserEntity::getRole, RoleEntity::getPermissions)
                .stream()
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Получть пользователя со списком его ролей по email или login
     */
    private UserEntity getUserWithRolesAndPermissionsOrThrow(String login) {
        return userRepository.findByEmailWithRolesAndPermissions(login)
                .or(
                        () -> userRepository.findByUsernameWithRolesAndPermissions(login)
                )
                .orElseThrow(
                        () -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND)
                );
    }

}
