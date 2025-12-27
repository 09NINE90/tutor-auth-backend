package ru.razumoff.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Полный профиль пользователя с персональными данными и ролями")
public class UserProfileResponse {

    @Schema(
            description = "Уникальный идентификатор пользователя",
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private UUID id;

    @Schema(
            description = "Электронная почта пользователя (уникальная)",
            example = "user@example.com"
    )
    private String email;

    @Schema(
            description = "Массив ролей пользователя в системе",
            example = "['STUDENT', 'TEACHER']"
    )
    private String[] roles;

    @Schema(
            description = "Имя пользователя",
            example = "Иван"
    )
    private String firstName;

    @Schema(
            description = "Фамилия пользователя",
            example = "Петров"
    )
    private String lastName;

    @Schema(
            description = "Отчество пользователя (необязательное)",
            example = "Иванович"
    )
    private String middleName;

    @Schema(
            description = "Дата рождения пользователя",
            example = "2000-05-15",
            type = "string",
            format = "date"
    )
    private LocalDate birthDate;

    @Schema(
            description = "Пол пользователя (Мужской, Женский, Не указан)",
            example = "Мужской",
            allowableValues = {"Мужской", "Женский", "Не указан"}
    )
    private String gender;

    @Schema(
            description = "URL аватара пользователя",
            example = "https://example.com/avatars/user123.jpg"
    )
    private String avatarUrl;
}
