package ru.razumoff.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.razumoff.dto.integration.ProfileRsDto;
import ru.razumoff.config.security.JwtUserPrincipal;
import ru.razumoff.dao.dto.internal.SearchUserDto;
import ru.razumoff.dao.dto.response.AvatarResponse;
import ru.razumoff.dao.dto.response.UserProfileResponse;
import ru.razumoff.dao.dto.response.UserSearchRsDto;
import ru.razumoff.service.ISearchUserService;
import ru.razumoff.service.IUserService;

import java.util.List;
import java.util.UUID;

import static ru.razumoff.Constants.ApiDocs.USER_TAG_DESCRIPTION;
import static ru.razumoff.Constants.ApiDocs.USER_TAG_NAME;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = USER_TAG_NAME, description = USER_TAG_DESCRIPTION)
public class UserApi {

    private final IUserService userService;
    private final ISearchUserService searchUserService;

    @GetMapping("/profile")
    @Operation(summary = "Получить данные в профиль пользователя")
    public ResponseEntity<UserProfileResponse> getUserById(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(userService.getUserProfileById(principal.getId()));
    }

    @PostMapping("/upload-avatar")
    @Operation(summary = "Обновить аватар пользователя")
    public ResponseEntity<AvatarResponse> uploadBaseImage(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @NotNull @RequestParam("image") MultipartFile avatarFile) {
        return ResponseEntity.ok(userService.uploadAvatar(principal.getId(), avatarFile));
    }

    @GetMapping("/{course_id}/search")
    public ResponseEntity<List<UserSearchRsDto>> searchUsers(
            @PathVariable("course_id") UUID courseId,
            @RequestParam("query") String searchQuery,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "10") int limit) {
        SearchUserDto request = SearchUserDto.builder()
                .userId(principal.getId())
                .courseId(courseId)
                .searchQuery(searchQuery)
                .limit(limit)
                .build();
        return ResponseEntity.ok(searchUserService.searchByFio(request));
    }

    @PostMapping("/profiles")
    public ResponseEntity<List<ProfileRsDto>> getUserProfiles(@RequestBody List<UUID> userIds) {
        return ResponseEntity.ok(userService.getUserProfilesByIds(userIds));
    }
}
