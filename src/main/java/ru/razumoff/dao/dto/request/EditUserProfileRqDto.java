package ru.razumoff.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EditUserProfileRqDto {

    @Schema(
            description = "Имя пользователя",
            example = "Иван",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Имя не может быть пустым.")
    @Size(min = 2, max = 50, message = "Имя должно содержать 2-50 символов")
    private String firstName;

    @Schema(
            description = "Фамилия пользователя",
            example = "Петров",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Фамилия не может быть пустым.")
    @Size(min = 2, max = 50, message = "Фамилия должна содержать 2-50 символов")
    private String lastName;

    @Schema(
            description = "Отчество пользователя (необязательное)",
            example = "Иванович",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(max = 50, message = "Отчество не должно превышать 50 символов")
    private String middleName;

}
