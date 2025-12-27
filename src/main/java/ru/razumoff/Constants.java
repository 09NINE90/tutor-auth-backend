package ru.razumoff;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    @UtilityClass
    public class ApiDocs {
        public static final String AUTH_TAG_NAME = "Аутентификация и авторизация";
        public static final String AUTH_TAG_DESCRIPTION = "Управление входом, регистрацией, восстановлением пароля и токенами доступа";
        public static final String USER_TAG_NAME = "Профили пользователей";
        public static final String USER_TAG_DESCRIPTION = "Управление данными профилей";
    }
}
