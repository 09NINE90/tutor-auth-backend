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

    @UtilityClass
    public static class YandexOauth2 {
        public static final String YA_ID = "id";
        public static final String YA_LOGIN = "login";
        public static final String YA_EMAIL = "default_email";
        public static final String YA_FIRST_NAME = "first_name";
        public static final String YA_LAST_NAME = "last_name";
        public static final String YA_AVATAR = "default_avatar_id";
        public static final String YA_GENDER = "sex";
        public static final String YA_GENDER_MALE = "male";
        public static final String YA_GENDER_FEMALE = "female";

    }

    @UtilityClass
    public static class GoogleOauth2 {
        public static final String GOOGLE_ID = "sub";
        public static final String GOOGLE_LOGIN = "name";
        public static final String GOOGLE_EMAIL = "email";
        public static final String GOOGLE_FIRST_NAME = "given_name";
        public static final String GOOGLE_LAST_NAME = "family_name";
        public static final String GOOGLE_AVATAR = "picture";
    }

    @UtilityClass
    public class Minio {
        public static final String PUBLIC_READ_POLICY_TEMPLATE = """
            {
              "Version": "2012-10-17",
              "Statement": [{
                "Effect": "Allow",
                "Principal": "*",
                "Action": ["s3:GetObject"],
                "Resource": ["arn:aws:s3:::%s/*"]
              }]
            }
            """;
    }
}
