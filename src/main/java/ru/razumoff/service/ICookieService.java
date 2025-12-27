package ru.razumoff.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ICookieService {

    void addRefreshCookie(HttpServletResponse response, String refreshToken);

    void deleteRefreshCookie(HttpServletResponse response);
}
