package ru.razumoff.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Запрос для аутентификации пользователя по email и паролю")
public class LoginRequest {

    @Schema(
            description = "Электронная почта пользователя для входа в систему",
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
}
