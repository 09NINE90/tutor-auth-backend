package ru.razumoff.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.razumoff.dao.enumz.AuthEventType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthEventEntity {

    /** Уникальный идентификатор события аутентификации */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Пользователь, связанный с событием (null при failed login) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    /** Тип события (LOGIN, LOGOUT, FAILED_LOGIN и т.д.) */
    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthEventType eventType;

    /** Email, использованный при аутентификации */
    @Column(name = "email", nullable = false)
    private String email;

    /** IP-адрес, с которого было совершено событие */
    @Column(name = "ip_address")
    private String ipAddress;

    /** User-Agent браузера */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** Дополнительная информация об устройстве */
    @Column(name = "device_info")
    private String deviceInfo;

    /** Геолокация (если доступна) */
    @Column(name = "location")
    private String location;

    /** Код ошибки (при неудачной аутентификации) */
    @Column(name = "error_code")
    private String errorCode;

    /** Сообщение об ошибке (при неудачной аутентификации) */
    @Column(name = "error_message")
    private String errorMessage;

    /** Дата и время события */
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

}

