package ru.razumoff.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity {

    /** Уникальный идентификатор refresh-токена */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Пользователь, которому принадлежит токен */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** Хеш refresh-токена (хранится хеш, а не сам токен) */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /** Информация об устройстве */
    @Column(name = "device_info")
    private String deviceInfo;

    /** IP-адрес, с которого был получен токен */
    @Column(name = "ip_address")
    private String ipAddress;

    /** User-Agent браузера */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** Дата и время истечения срока действия токена */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** Дата и время создания токена */
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    /** Дата и время отзыва токена (null, если токен ещё активен) */
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    /** Ссылка на новый токен, заменивший данный (при ротации) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by")
    private RefreshTokenEntity replacedBy;
}
