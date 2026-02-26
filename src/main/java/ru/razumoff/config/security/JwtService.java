package ru.razumoff.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaim(token, Claims::getSubject));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Set<String> extractPermissions(String token) {
        return extractClaim(token, claims -> {
            Object raw = claims.get("permissions");
            return convertToSet(raw);
        });
    }

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


    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isTokenValid(String token) {
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

    public String generateAccessToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email, accessExpirationMs);
    }

    public String generateRefreshToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email, refreshExpirationMs);
    }

    public String generateAccessToken(UUID userId, String email,
                                      String role,
                                      Collection<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role);
        claims.put("permissions", permissions);
        return createToken(claims, String.valueOf(userId), accessExpirationMs);
    }

    public String generateRefreshToken(UUID userId, String email,
                                       String role,
                                       Collection<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role);
        claims.put("permissions", permissions);
        return createToken(claims, String.valueOf(userId), refreshExpirationMs);
    }


    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(String token, String email) {
        try {
            final String username = extractUsername(token);
            return (username.equals(email) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
}
