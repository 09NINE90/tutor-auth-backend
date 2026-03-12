package ru.razumoff.dao.dto.internal;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class TokensAndPermissionsDto {

    private String accessToken;

    private String refreshToken;

    private Set<String> permissions;
}
