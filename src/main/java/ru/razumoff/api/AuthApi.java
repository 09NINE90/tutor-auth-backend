package ru.razumoff.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.razumoff.dao.dto.request.LoginRequest;
import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.dto.response.AuthRsDto;
import ru.razumoff.service.IAuthService;

import static ru.razumoff.Constants.ApiDocs.AUTH_TAG_DESCRIPTION;
import static ru.razumoff.Constants.ApiDocs.AUTH_TAG_NAME;
import static ru.razumoff.Constants.REFRESH_COOKIE_NAME;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = AUTH_TAG_NAME, description = AUTH_TAG_DESCRIPTION)
public class AuthApi {

    private final IAuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Зарегистрировать пользователя в системе")
    public ResponseEntity<AuthRsDto> register(@Valid @RequestBody RegisterRequest request) {
        AuthRsDto result = authService.register(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему")
    public ResponseEntity<AuthRsDto> login(@Valid @RequestBody LoginRequest request) {
        AuthRsDto result = authService.login(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из системы")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление токена доступа")
    public ResponseEntity<AuthRsDto> refresh(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        AuthRsDto result = authService.refresh(refreshToken);
        return ResponseEntity.ok(result);
    }

}

