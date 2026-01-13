package ru.razumoff.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ответ после загрузки аватара пользователем")
public class AvatarResponse {

    @Schema(
            description = "Ссылка на аватар пользователя",
            example = "https://example.com/image.png"
    )
    private String imageUrl;

}
