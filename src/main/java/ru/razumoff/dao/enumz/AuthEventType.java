package ru.razumoff.dao.enumz;

import lombok.Getter;

@Getter
public enum AuthEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    REGISTER_SUCCESS,
    REGISTER_FAILED,
    REFRESH_SUCCESS,
    REFRESH_FAILED,
    LOGOUT,
    TOKEN_REVOKED
}