package ru.razumoff.service;

import ru.razumoff.dao.dto.request.LoginRequest;
import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.dto.response.AuthRsDto;

public interface IAuthService {
    AuthRsDto register(RegisterRequest request);

    AuthRsDto login(LoginRequest request);

    AuthRsDto refresh(String refreshToken);

    void logout(String refreshToken);
}
