package ru.razumoff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.razumoff.commonlib.exceptions.ErrorCode;
import ru.razumoff.commonlib.exceptions.PlatformException;
import ru.razumoff.config.security.JwtService;
import ru.razumoff.dao.dto.request.LoginRequest;
import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.dto.response.TokenResponse;
import ru.razumoff.dao.entity.UserEntity;
import ru.razumoff.dao.repository.UserRepository;
import ru.razumoff.service.IAuthService;
import ru.razumoff.service.IUserProfileService;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final IUserProfileService userProfileService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Регистрация нового пользователя STUDENT
     * Создает UserEntity + Profile + JWT токены
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        try {
            UserEntity user = new UserEntity();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRoles(new String[]{"STUDENT"});
            user.setEnabled(true);
            user.setCreatedAt(OffsetDateTime.now());

            UserEntity savedUser = userRepository.save(user);
            userProfileService.addUserProfile(user.getId(), request);

            String access = jwtService.generateAccessToken(
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getRoles()
            );
            String refresh = jwtService.generateRefreshToken(
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getRoles()
            );

            return new TokenResponse(access, refresh, user.getRoles());
        } catch (Exception e) {
            log.error("Registration failed for {}: {}", request.getEmail(), e.getMessage());
            throw new PlatformException(ErrorCode.AUTH_REGISTRATION_FAILED);
        }
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

            UserEntity user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                    () -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND)
            );

            String access = jwtService.generateAccessToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRoles()
            );
            String refresh = jwtService.generateRefreshToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRoles()
            );

            return new TokenResponse(access, refresh, user.getRoles());
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

        String newAccess = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRoles()
        );
        String newRefresh = jwtService.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                user.getRoles()
        );

        // TODO: проверить в БД, не отозван ли
        // refreshTokenRepository.findActiveByToken(refreshToken).orElseThrow();

        // TODO: сохранить новый в БД и пометить старый как использованный
        return new TokenResponse(newAccess, newRefresh, user.getRoles());
    }
}
