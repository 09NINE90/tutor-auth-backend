package ru.razumoff.dao.dto.internal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomUserOauthInfo {
    private String externalId;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String gender;
}
