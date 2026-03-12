package ru.razumoff.service.impl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import ru.razumoff.service.ICookieService;

import java.time.Duration;

import static ru.razumoff.Constants.REFRESH_COOKIE_NAME;


@Service
@RequiredArgsConstructor
public class CookieService implements ICookieService {

    private final HttpServletResponse response;

    /**
     * Установка refresh токена в httpOnly cookie (7 дней)
     */
    public void addRefreshCookie(String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false) // todo для локальной разработки
                .path("/")
                .sameSite("Strict")
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Удаление refresh cookie (maxAge=0)
     */
    public void deleteRefreshCookie() {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false) // todo для локальной разработки
                .path("/")
                .sameSite("Strict")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
