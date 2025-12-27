package ru.razumoff.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.razumoff.config.security.JwtUserPrincipal;
import ru.razumoff.dao.dto.response.UserProfileResponse;
import ru.razumoff.service.IUserService;

import static ru.razumoff.Constants.ApiDocs.USER_TAG_DESCRIPTION;
import static ru.razumoff.Constants.ApiDocs.USER_TAG_NAME;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = USER_TAG_NAME, description = USER_TAG_DESCRIPTION)
public class UserController {

    private final IUserService userService;


    @GetMapping("/profile")
    @Operation(summary = "Получить данные в профиль пользователя")
    public ResponseEntity<UserProfileResponse> getUserById(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(userService.getUserProfileById(principal.getId()));
    }
}
