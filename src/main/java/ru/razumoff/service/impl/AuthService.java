package ru.razumoff.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.razumoff.config.security.JwtService;
import ru.razumoff.dao.entity.*;
import ru.razumoff.dao.enumz.AuthEventType;
import ru.razumoff.dao.repository.AuthEventRepository;
import ru.razumoff.dao.repository.RefreshTokenRepository;
import ru.razumoff.exceptions.ErrorCode;
import ru.razumoff.exceptions.PlatformException;
import ru.razumoff.dao.dto.request.LoginRequest;
import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.dto.response.TokenResponse;
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
    private final HttpServletRequest request;
    private final AuthEventRepository authEventRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Регистрация нового пользователя STUDENT
     * Создает UserEntity + Profile + JWT токены
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = request.getEmail().trim();

        try {
            if (userRepository.findByEmail(email).isPresent()) {
                throw new PlatformException(ErrorCode.AUTH_USER_EXISTS);
            }

            UserEntity user = new UserEntity();
            user.setEmail(email);
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

            saveAuthEvent(savedUser, email, AuthEventType.REGISTER_SUCCESS, null, null);
            saveRefreshToken(savedUser, refresh, getDeviceInfo());

            log.info("User registered success: {}", user);
            return new TokenResponse(access, refresh, null);
        } catch (PlatformException e) {
            saveAuthEvent(null, email, AuthEventType.REGISTER_FAILED,
                    e.getErrorCode().name(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Registration failed for {}: {}", request.getEmail(), e.getMessage());
            saveAuthEvent(null, email, AuthEventType.REGISTER_FAILED,
                    ErrorCode.AUTH_REGISTRATION_FAILED.name(), e.getMessage());
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
        String email = request.getEmail().trim();

        try {
            var authToken = new UsernamePasswordAuthenticationToken(
                    email, request.getPassword()
            );
            authenticationManager.authenticate(authToken);

            UserEntity user = getUserWithRolesAndPermissionsOrThrow(email);

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

            saveAuthEvent(user, email, AuthEventType.LOGIN_SUCCESS, null, null);
            saveRefreshToken(user, refresh, getDeviceInfo());

            log.info("User login success: {}", user);
            return new TokenResponse(access, refresh, null);
        } catch (AuthenticationException ex) {
            log.warn("Login failed for {}: invalid credentials", request.getEmail());
            saveAuthEvent(null, email, AuthEventType.LOGIN_FAILED,
                    ErrorCode.AUTH_INVALID_CREDENTIALS.name(), "Invalid credentials");
            throw new PlatformException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        } catch (Exception e) {
            log.error("Login failed for {}: {}", request.getEmail(), e.getMessage());
            saveAuthEvent(null, email, AuthEventType.LOGIN_FAILED,
                    ErrorCode.INTERNAL_ERROR.name(), e.getMessage());
            throw new PlatformException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Обновление access токена по refresh токену
     * Выдает новую пару access+refresh токенов
     */
    public TokenResponse refresh(String refreshToken) {
        String oldRefreshToken = refreshToken;

        try {
            if (!jwtService.isTokenValid(refreshToken)) {
                saveAuthEvent(null, null, AuthEventType.REFRESH_FAILED,
                        ErrorCode.AUTH_REFRESH_INVALID.name(), "Invalid refresh token");
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

            revokeOldRefreshToken(oldRefreshToken);
            saveRefreshToken(user, newRefresh, getDeviceInfo());
            saveAuthEvent(user, user.getEmail(), AuthEventType.REFRESH_SUCCESS, null, null);

            log.info("Token refreshed success. userId {}", userId);
            return new TokenResponse(newAccess, newRefresh, null);

        } catch (PlatformException e) {
            saveAuthEvent(null, null, AuthEventType.REFRESH_FAILED,
                    e.getErrorCode().name(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            saveAuthEvent(null, null, AuthEventType.REFRESH_FAILED,
                    ErrorCode.INTERNAL_ERROR.name(), e.getMessage());
            throw new PlatformException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        try {
            if (refreshToken != null && !refreshToken.isEmpty()) {
                String tokenHash = DigestUtils.sha256Hex(refreshToken);
                refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                    token.setRevokedAt(OffsetDateTime.now());
                    refreshTokenRepository.save(token);

                    saveAuthEvent(token.getUser(), token.getUser().getEmail(),
                            AuthEventType.LOGOUT, null, null);

                    log.info("User {} logged out successfully", token.getUser().getId());
                });
            }
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
        }
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

    /**
     * Сохраняет событие аутентификации в историю.
     * Метод перехватывает все исключения при сохранении, чтобы не прерывать основной поток выполнения.
     *
     * @param user         пользователь, совершивший действие (может быть null для неаутентифицированных событий)
     * @param email        email пользователя на момент события
     * @param eventType    тип события аутентификации (LOGIN_SUCCESS, LOGIN_FAILED, и т.д.)
     * @param errorCode    код ошибки (для неуспешных событий)
     * @param errorMessage детальное сообщение об ошибке (для неуспешных событий)
     */
    private void saveAuthEvent(UserEntity user, String email, AuthEventType eventType,
                               String errorCode, String errorMessage) {
        try {
            AuthEventEntity event = AuthEventEntity.builder()
                    .user(user)
                    .email(email != null ? email : (user != null ? user.getEmail() : "unknown"))
                    .eventType(eventType)
                    .ipAddress(getClientIp())
                    .userAgent(getUserAgent())
                    .deviceInfo(getDeviceInfo())
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .createdAt(OffsetDateTime.now())
                    .build();
            authEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to save auth event: {}", e.getMessage());
        }
    }

    /**
     * Сохраняет refresh токен в базе данных для последующего использования и управления сессиями.
     * Токен сохраняется в виде хеша (SHA-256) для безопасности.
     * Устанавливает срок действия токена - 30 дней с момента создания.
     *
     * @param user       пользователь, которому принадлежит токен
     * @param token      оригинальный refresh токен (будет захеширован перед сохранением)
     * @param deviceInfo информация об устройстве пользователя (из User-Agent)
     */
    private void saveRefreshToken(UserEntity user, String token, String deviceInfo) {
        try {
            String tokenHash = DigestUtils.sha256Hex(token);

            RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .deviceInfo(deviceInfo)
                    .ipAddress(getClientIp())
                    .userAgent(getUserAgent())
                    .expiresAt(OffsetDateTime.now().plusDays(30))
                    .createdAt(OffsetDateTime.now())
                    .build();
            refreshTokenRepository.save(refreshToken);
        } catch (Exception e) {
            log.error("Failed to save refresh token: {}", e.getMessage());
        }
    }

    /**
     * Отзывает старый refresh токен при обновлении токенов (refresh).
     * Помечает токен как отозванный (revoked), устанавливая дату отзыва.
     * Используется для инвалидации старого токена после выдачи нового.
     *
     * @param oldRefreshToken старый refresh токен, который необходимо отозвать
     */
    private void revokeOldRefreshToken(String oldRefreshToken) {
        if (oldRefreshToken != null) {
            try {
                String tokenHash = DigestUtils.sha256Hex(oldRefreshToken);
                refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                    token.setRevokedAt(OffsetDateTime.now());
                    refreshTokenRepository.save(token);
                });
            } catch (Exception e) {
                log.error("Failed to revoke old refresh token: {}", e.getMessage());
            }
        }
    }

    /**
     * Определяет реальный IP-адрес клиента с учетом прокси и балансировщиков нагрузки.
     * Проверяет заголовки в следующем порядке:
     * 1. X-Forwarded-For - стандартный заголовок для прокси
     * 2. X-Real-IP - часто используется в Nginx
     * 3. request.getRemoteAddr() - прямой IP, если нет прокси
     *
     * @return строковое представление IP-адреса клиента
     */
    private String getClientIp() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Получает значение заголовка User-Agent из HTTP запроса.
     * User-Agent содержит информацию о браузере, операционной системе и устройстве клиента.
     *
     * @return строка User-Agent или null, если заголовок отсутствует
     */
    private String getUserAgent() {
        return request.getHeader("User-Agent");
    }

    /**
     * Формирует информацию об устройстве клиента на основе User-Agent.
     * Очищает User-Agent от управляющих символов и обрезает до 255 символов
     * для безопасного хранения в базе данных.
     *
     * @return очищенная и обрезанная информация об устройстве (макс. 255 символов),
     * или "Unknown" если User-Agent отсутствует
     */
    private String getDeviceInfo() {
        String userAgent = getUserAgent();
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }
        String cleanUserAgent = userAgent.replaceAll("[\\p{Cntrl}]]", " ");
        return cleanUserAgent.substring(0, Math.min(cleanUserAgent.length(), 255));
    }
}
