package ru.razumoff.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ответ с токенами JWT доступа и обновления, а также ролями пользователя")
public class AuthRsDto {

    @Schema(
            description = "JWT токен доступа для аутентификации запросов",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String accessToken;

    @Schema(
            description = "Список разрешений пользователя"
    )
    private Set<String> permissions;

}