package ru.razumoff.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.razumoff.dao.entity.UserEntity;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * Сервис для работы с JWT (JSON Web Token).
 * <p>
 * Отвечает за генерацию, валидацию и извлечение данных из JWT токенов.
 * Поддерживает два типа токенов: access (краткосрочный) и refresh (долгосрочный).
 * </p>
 * <p>
 * Использует алгоритм HMAC-SHA с симметричным ключом из конфигурации.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Получает секретный ключ для подписи JWT из строки секрета.
     *
     * @return {@link SecretKey} для подписи и проверки JWT
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Извлекает дату истечения срока действия токена.
     *
     * @param token JWT токен
     * @return {@link Date} истечения токена
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Извлекает идентификатор пользователя из токена (поле 'sub').
     *
     * @param token JWT токен
     * @return {@link UUID} идентификатор пользователя
     */
    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaim(token, Claims::getSubject));
    }

    /**
     * Извлекает имя пользователя из токена (кастомное поле 'username').
     *
     * @param token JWT токен
     * @return имя пользователя
     */
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

    /**
     * Извлекает роль пользователя из токена (кастомное поле 'role').
     *
     * @param token JWT токен
     * @return название роли
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Извлекает список прав (permissions) пользователя из токена.
     * <p>
     * Поддерживает разные форматы хранения: коллекция или строка.
     * </p>
     *
     * @param token JWT токен
     * @return {@link Set} прав пользователя
     */
    public Set<String> extractPermissions(String token) {
        return extractClaim(token, claims -> {
            Object raw = claims.get("permissions");
            return convertToSet(raw);
        });
    }

    /**
     * Преобразует сырое значение из JWT claims в {@link Set}.
     * <p>
     * Поддерживает {@link Collection} и {@link String} форматы.
     * </p>
     *
     * @param raw сырое значение из JWT
     * @return {@link Set} прав
     */
    @SuppressWarnings("unchecked")
    private Set<String> convertToSet(Object raw) {
        if (raw instanceof Collection<?> collection) {
            return new HashSet<>((Collection<String>) collection);
        }
        if (raw instanceof String s) {
            return Set.of(s);
        }
        return Set.of();
    }


    /**
     * Извлекает конкретный claim из токена с помощью переданной функции.
     *
     * @param token           JWT токен
     * @param claimsResolver  функция для извлечения нужного поля из claims
     * @param <T>             тип возвращаемого значения
     * @return извлечённое значение claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Извлекает все claims из JWT токена.
     * <p>
     * Парсит токен и проверяет подпись с помощью секретного ключа.
     * </p>
     *
     * @param token JWT токен
     * @return {@link Claims} все claims токена
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Проверяет, истёк ли срок действия токена.
     *
     * @param token JWT токен
     * @return true, если токен истёк
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Проверяет валидность JWT токена.
     * <p>
     * Токен считается валидным, если он не пустой, имеет корректную подпись
     * и срок действия не истёк.
     * </p>
     *
     * @param token JWT токен
     * @return true, если токен валиден
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.isEmpty()) return false;
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return !claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Генерирует access токен только для имени пользователя.
     * <p>
     * Используется в упрощённых сценариях, когда не требуются роль и права.
     * </p>
     *
     * @param username имя пользователя
     * @return JWT access токен
     */
    public String generateAccessToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, accessExpirationMs);
    }

    /**
     * Генерирует refresh токен только для имени пользователя.
     * <p>
     * Используется в упрощённых сценариях, когда не требуются роль и права.
     * </p>
     *
     * @param username имя пользователя
     * @return JWT refresh токен
     */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, refreshExpirationMs);
    }

    /**
     * Генерирует access токен на основе {@link UserEntity}.
     * <p>
     * Включает в claims username, роль и все права пользователя.
     * </p>
     *
     * @param user сущность пользователя
     * @return JWT access токен с полной информацией
     */
    public String generateAccessToken(UserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().getName());
        claims.put("permissions", user.getPermissions());
        return createToken(claims, String.valueOf(user.getId()), accessExpirationMs);
    }

    /**
     * Генерирует refresh токен на основе {@link UserEntity}.
     * <p>
     * Включает в claims username, роль и все права пользователя.
     * </p>
     *
     * @param user сущность пользователя
     * @return JWT refresh токен с полной информацией
     */
    public String generateRefreshToken(UserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().getName());
        claims.put("permissions", user.getPermissions());
        return createToken(claims, String.valueOf(user.getId()), refreshExpirationMs);
    }

    /**
     * Генерирует access токен из отдельных параметров.
     * <p>
     * Полезен для сценариев, когда пользователь собирается вручную или
     * при миграции между форматами токенов.
     * </p>
     *
     * @param userId      идентификатор пользователя
     * @param username    имя пользователя
     * @param role        роль пользователя
     * @param permissions список прав
     * @return JWT access токен
     */
    public String generateAccessToken(UUID userId, String username,
                                      String role,
                                      Collection<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username.trim());
        claims.put("role", role);
        claims.put("permissions", permissions);
        return createToken(claims, String.valueOf(userId), accessExpirationMs);
    }

    /**
     * Генерирует refresh токен из отдельных параметров.
     * <p>
     * Полезен для сценариев, когда пользователь собирается вручную или
     * при миграции между форматами токенов.
     * </p>
     *
     * @param userId      идентификатор пользователя
     * @param username    имя пользователя
     * @param role        роль пользователя
     * @param permissions список прав
     * @return JWT refresh токен
     */
    public String generateRefreshToken(UUID userId, String username,
                                       String role,
                                       Collection<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username.trim());
        claims.put("role", role);
        claims.put("permissions", permissions);
        return createToken(claims, String.valueOf(userId), refreshExpirationMs);
    }


    /**
     * Создаёт JWT токен с указанными claims, subject и временем жизни.
     *
     * @param claims       кастомные claims для включения в токен
     * @param subject      основной subject токена (обычно ID пользователя)
     * @param expirationMs время жизни токена в миллисекундах
     * @return подписанный JWT токен
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Валидирует токен и проверяет соответствие имени пользователя.
     * <p>
     * Используется в сценариях, когда нужно убедиться, что токен принадлежит
     * конкретному пользователю (например, при refresh flow).
     * </p>
     *
     * @param token    JWT токен
     * @param userName ожидаемое имя пользователя
     * @return true, если токен валиден и имя совпадает
     */
    public Boolean validateToken(String token, String userName) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userName) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
}
