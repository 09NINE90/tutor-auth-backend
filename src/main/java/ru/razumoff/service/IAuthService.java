package ru.razumoff.service;

import ru.razumoff.dao.dto.request.LoginRequest;
import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.dto.response.TokenResponse;

public interface IAuthService {
    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(String refreshToken);

    void logout(String refreshToken);
}
