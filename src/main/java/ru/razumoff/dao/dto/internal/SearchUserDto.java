package ru.razumoff.dao.dto.internal;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SearchUserDto {
    private UUID userId;
    private UUID courseId;
    private String searchQuery;
    private int limit;
}
