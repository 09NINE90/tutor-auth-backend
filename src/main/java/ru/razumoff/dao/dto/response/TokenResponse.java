package ru.razumoff.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ответ с токенами JWT доступа и обновления, а также ролями пользователя")
public class TokenResponse {

    @Schema(
            description = "JWT токен доступа для аутентификации запросов",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String accessToken;

    @Schema(
            description = "JWT токен обновления для получения нового access токена",
            example = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..."
    )
    private String refreshToken;

    @Schema(
            description = "Массив ролей пользователя, полученных после аутентификации",
            example = "['USER', 'STUDENT']"
    )
    private String[] roles;
}