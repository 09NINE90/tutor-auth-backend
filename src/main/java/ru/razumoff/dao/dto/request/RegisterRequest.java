package ru.razumoff.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import ru.razumoff.dao.enumz.GenderType;

import java.time.LocalDate;

@Data
@Builder
public class RegisterRequest {

    @Schema(
            description = "Электронная почта пользователя для регистрации в системе",
            example = "user@example.com",
            maxLength = 254
    )
    @NotBlank(message = "Email не может быть пустым.")
    @Email(message = "Неверный формат email")
    private String email;

    @Schema(
            description = "Пароль пользователя (минимум 10 символов)",
            example = "MySecurePassword123",
            minLength = 10
    )
    @NotBlank(message = "Пароль не может быть пустым.")
    @Size(min = 10, message = "Пароль должен содержать минимум 10 символов.")
    private String password;

    @Schema(
            description = "Имя пользователя",
            example = "Иван"
    )
    @NotBlank(message = "Имя не может быть пустым.")
    @Size(min = 2, max = 50, message = "Имя должно содержать 2-50 символов")
    private String firstName;

    @Schema(
            description = "Фамилия пользователя",
            example = "Петров"
    )
    @NotBlank(message = "Фамилия не может быть пустым.")
    @Size(min = 2, max = 50, message = "Фамилия должна содержать 2-50 символов")
    private String lastName;

    @Schema(
            description = "Отчество пользователя (необязательное)",
            example = "Иванович"
    )
    @Size(max = 50, message = "Отчество не должно превышать 50 символов")
    private String middleName;

    @Schema(
            description = "Дата рождения пользователя",
            example = "2000-05-15",
            type = "string",
            format = "date"
    )
    @NotNull(message = "Дата рождения обязательна.")
    @Past(message = "Дата рождения должна быть в прошлом")
    private LocalDate birthDate;

    @Schema(
            description = "Пол пользователя",
            example = "MALE",
            enumAsRef = true
    )
    private GenderType gender;

    @Schema(
            description = "URL аватара пользователя",
            example = "https://example.com/avatars/user123.jpg"
    )
    @URL(message = "Неверный формат URL аватара")
    private String avatarUrl;
}