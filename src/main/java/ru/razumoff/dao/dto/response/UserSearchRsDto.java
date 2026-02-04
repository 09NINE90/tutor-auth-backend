package ru.razumoff.dao.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchRsDto {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarS3Key;
}
