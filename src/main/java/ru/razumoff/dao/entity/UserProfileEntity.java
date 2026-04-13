package ru.razumoff.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.razumoff.dao.enumz.GenderType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users_profiles")
public class UserProfileEntity {

    /** Уникальный идентификатор профиля */
    @Id
    @GeneratedValue
    private UUID id;

    /** ID пользователя, к которому относится профиль (внешний ключ без JPA-связи) */
    @Column(unique = true)
    private UUID userId;

    /**
     * Имя
     */
    @Column(name = "first_name")
    private String firstName;

    /**
     * Фамилия
     */
    @Column(name = "last_name")
    private String lastName;

    /**
     * Отчество
     */
    @Column(name = "middle_name")
    private String middleName;

    /**
     * Дата рождения
     */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /**
     * Гендер пользователя
     */
    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private GenderType gender;

    /**
     * Ключ аватара пользователя
     */
    @Column(name = "avatar_s3_key")
    private String avatarS3Key;

    /** Дата и время создания записи */
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /** Дата и время последнего обновления записи */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
