package ru.razumoff.service;

public interface ICookieService {

    void addRefreshCookie(String refreshToken);

    void deleteRefreshCookie();
}
