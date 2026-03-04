package ru.razumoff.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Запрос для аутентификации пользователя по login и паролю")
public class LoginRequest {

    @Schema(
            description = "Логин пользователя для входа в систему",
            example = "User_name_123",
            maxLength = 50
    )
    @Size(min = 3, max = 50, message = "Логин должен содержать от 3 до 50 символов")
    @NotBlank(message = "Логин не может быть пустым.")
    private String login;

    @Schema(
            description = "Пароль пользователя (минимум 10 символов)",
            example = "MySecurePassword123",
            minLength = 10
    )
    @NotBlank(message = "Пароль не может быть пустым.")
    @Size(min = 10, message = "Пароль должен содержать минимум 10 символов.")
    private String password;
}
