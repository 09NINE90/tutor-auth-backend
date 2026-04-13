package ru.razumoff.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class UserEntity {

    /** Уникальный идентификатор пользователя */
    @Id
    @GeneratedValue
    private UUID id;

    /** Внешний ID от OAuth2-провайдера (Google, Яндекс и т.д.) */
    @Column(unique = true)
    private String externalId;

    /** Логин (username) пользователя */
    @Column(nullable = false, unique = true)
    private String username;

    /** Электронная почта */
    @Column(unique = true)
    private String email;

    /** Хешированный пароль (null для OAuth2-пользователей) */
    private String password;

    /** Роль пользователя */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    /** Флаг активности учётной записи */
    @Column(nullable = false)
    private boolean enabled;

    /** Дата и время создания записи */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /** Дата и время последнего обновления записи */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Set<String> getPermissions() {
        return role != null && role.getPermissions() != null
                ? role.getPermissions().stream().map(PermissionEntity::getName).collect(Collectors.toSet())
                : Set.of();
    }
}
