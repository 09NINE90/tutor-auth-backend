package ru.razumoff.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.razumoff.exceptions.ErrorCode;
import ru.razumoff.exceptions.PlatformException;
import ru.razumoff.dao.dto.request.LoginRequest;
import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.dto.response.TokenResponse;
import ru.razumoff.service.IAuthService;
import ru.razumoff.service.ICookieService;

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
    private final ICookieService cookieService;


    @PostMapping("/register")
    @Operation(summary = "Зарегистрировать пользователя в системе")
    public ResponseEntity<TokenResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        TokenResponse tokens = authService.register(request);
        cookieService.addRefreshCookie(response, tokens.getRefreshToken());
        return ResponseEntity.ok(new TokenResponse(tokens.getAccessToken(), null, tokens.getRoles()));
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        TokenResponse tokens = authService.login(request);
        cookieService.addRefreshCookie(response, tokens.getRefreshToken());
        return ResponseEntity.ok(new TokenResponse(tokens.getAccessToken(), null, tokens.getRoles()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из системы")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        cookieService.deleteRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление токена доступа")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            cookieService.deleteRefreshCookie(response);
            throw new PlatformException(ErrorCode.AUTH_TOKEN_REQUIRED);
        }

        try {
            TokenResponse tokens = authService.refresh(refreshToken);
            cookieService.addRefreshCookie(response, tokens.getRefreshToken());
            return ResponseEntity.ok(new TokenResponse(tokens.getAccessToken(), null, tokens.getRoles()));
        } catch (PlatformException e) {
            log.warn("Refresh failed: {}", e.getMessage());
            cookieService.deleteRefreshCookie(response);
            throw e;
        }
    }

}

