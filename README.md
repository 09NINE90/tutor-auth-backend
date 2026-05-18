# Teacher Portal — Auth User Service

Сервис аутентификации и управления пользователями для платформы Teacher Portal.
Обеспечивает регистрацию, вход (login/password + OAuth2 Google/Яндекс), управление профилями,
загрузку аватаров и поиск пользователей.

---

- [Быстрый старт](#быстрый-старт)
- [Технологии](#технологии)
- [Структура проекта](#структура-проекта)
- [REST API](#rest-api)
- [Модель данных](#модель-данных)
- [Ролевая модель](#ролевая-модель)
- [Безопасность](#безопасность)
- [Миграции БД](#миграции-бд)
- [Конфигурация](#конфигурация)
- [Docker](#docker)
- [Swagger](#swagger)

---

## Быстрый старт

**Требования:** Java 25, PostgreSQL, MinIO

1. Скопируйте `template.env` → `.env` и заполните переменные
2. Запустите PostgreSQL и MinIO
3. `./gradlew bootRun` — сервис поднимется на `http://localhost:8081`

Миграции Flyway применятся автоматически при первом запуске.

---

## Технологии

| Категория        | Технологии                                                  |
|------------------|-------------------------------------------------------------|
| **Язык**         | Java 25                                                     |
| **Фреймворк**    | Spring Boot 4.0.6                                           |
| **Сборка**       | Gradle 9.5.0                                                |
| **БД**           | PostgreSQL + Flyway                                         |
| **Безопасность** | Spring Security, JWT (jjwt 0.12.6), OAuth2 (Google, Яндекс) |
| **Хранилище**    | MinIO (S3-совместимое, для аватаров)                        |
| **ORM**          | Spring Data JPA + Hibernate                                 |
| **Маппинг**      | ModelMapper 3.2.6                                           |
| **Документация** | SpringDoc OpenAPI (Swagger UI)                              |
| **Boilerplate**  | Lombok                                                      |

---

## Структура проекта

```
tutor-auth-backend/
├── src/main/java/ru/razumoff/
│   ├── TutorAuthApplication.java          # Точка входа (@SpringBootApplication)
│   ├── Constants.java                     # Константы OAuth, MinIO, cookie
│   │
│   ├── api/                               # REST-контроллеры
│   │   ├── AuthApi.java                   # /api/auth — регистрация, логин, refresh, logout
│   │   └── UserApi.java                   # /api/user — профиль, аватар, поиск
│   │
│   ├── config/                            # Spring-конфигурация
│   │   ├── MinioConfig.java               # Бин MinioClient
│   │   ├── OpenApiConfig.java             # Swagger/OpenAPI настройки
│   │   └── security/                      # Безопасность (подробный JavaDoc внутри)
│   │       ├── SecurityConfig.java        # Цепочка фильтров, CORS, OAuth2
│   │       ├── JwtAuthFilter.java         # JWT-фильтр (OncePerRequestFilter)
│   │       ├── JwtService.java            # Генерация/валидация JWT
│   │       ├── UserDetailsServiceImpl.java# UserDetailsService для login/password
│   │       └── CustomOAuth2UserService.java # Создание/обновление из OAuth2
│   │
│   ├── dao/
│   │   ├── dto/
│   │   │   ├── request/                   # DTO входящих запросов
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   └── EditUserProfileRqDto.java
│   │   │   ├── response/                  # DTO ответов
│   │   │   │   ├── AuthRsDto.java
│   │   │   │   ├── AvatarResponse.java
│   │   │   │   ├── EditUserProfileRsDto.java
│   │   │   │   ├── UserProfileResponse.java
│   │   │   │   └── UserSearchRsDto.java
│   │   │   └── internal/                  # Внутренние DTO
│   │   │       ├── CustomUserOauthInfo.java
│   │   │       ├── SearchUserDto.java
│   │   │       └── TokensAndPermissionsDto.java
│   │   ├── entity/                        # JPA-сущности (подробный JavaDoc внутри)
│   │   │   ├── UserEntity.java
│   │   │   ├── UserProfileEntity.java
│   │   │   ├── RoleEntity.java
│   │   │   ├── PermissionEntity.java
│   │   │   ├── RefreshTokenEntity.java
│   │   │   └── AuthEventEntity.java
│   │   ├── enumz/                         # Перечисления
│   │   │   ├── AuthEventType.java         # Типы auth-событий
│   │   │   └── GenderType.java            # MALE, FEMALE, UNKNOWN
│   │   └── repository/                    # Spring Data JPA репозитории
│   │       ├── UserRepository.java
│   │       ├── UserProfileRepository.java
│   │       ├── RoleRepository.java
│   │       ├── AuthEventRepository.java
│   │       └── RefreshTokenRepository.java
│   │
│   ├── exeptions/
│   │   └── GlobalExceptionHandler.java    # @RestControllerAdvice
│   │
│   ├── minio/                             # Работа с MinIO
│   │   ├── IMinioFileService.java
│   │   └── MinioFileService.java
│   │
│   └── service/                           # Бизнес-логика
│       ├── IAuthService.java
│       ├── ICookieService.java
│       ├── IUserService.java
│       ├── IUserProfileService.java
│       ├── ISearchUserService.java
│       └── impl/
│           ├── AuthService.java           # Регистрация, логин, refresh, logout
│           ├── CookieService.java         # HTTP-only cookie с refresh токеном
│           ├── UserService.java           # Профиль, аватар, batch, редактирование
│           ├── UserProfileService.java    # CRUD профилей
│           └── SearchUserService.java     # Триграммный поиск по ФИО
│
├── src/main/resources/
│   ├── application.yml                    # Основная конфигурация
│   └── db/migration/                      # Flyway-миграции (V1–V13)
│
├── build.gradle                           # Зависимости и сборка
├── Dockerfile                             # Образ для деплоя
├── .env                                   # Переменные окружения (локальные)
└── template.env                           # Шаблон .env
```

---

## REST API

### Аутентификация (`/api/auth`)

| Метод  | Путь                 | Описание                                       | Доступ    |
|--------|----------------------|------------------------------------------------|-----------|
| `POST` | `/api/auth/register` | Регистрация (email, username, пароль, профиль) | Публичный |
| `POST` | `/api/auth/login`    | Вход по логину/паролю                          | Публичный |
| `POST` | `/api/auth/logout`   | Выход (отзыв refresh токена)                   | Публичный |
| `POST` | `/api/auth/refresh`  | Обновление access токена                       | Публичный |

### Пользователи (`/api/user`)

| Метод  | Путь                           | Описание                         | Доступ                                  |
|--------|--------------------------------|----------------------------------|-----------------------------------------|
| `GET`  | `/api/user/profile`            | Получить свой профиль            | Аутентифицированный                     |
| `PUT`  | `/api/user/profile`            | Редактировать ФИО                | Аутентифицированный (`USER_UPDATE_OWN`) |
| `POST` | `/api/user/upload-avatar`      | Загрузить аватар (image, до 5MB) | Аутентифицированный                     |
| `GET`  | `/api/user/{course_id}/search` | Поиск по ФИО (pg_trgm)           | Аутентифицированный                     |
| `POST` | `/api/user/profiles`           | Batch получение профилей по ID   | Аутентифицированный                     |

### OAuth2

| Путь                           | Описание                |
|--------------------------------|-------------------------|
| `/oauth2/authorization/google` | Инициация Google OAuth2 |
| `/oauth2/authorization/yandex` | Инициация Яндекс OAuth2 |
| `/login/oauth2/code/google`    | Callback Google         |
| `/login/oauth2/code/yandex`    | Callback Яндекс         |

После успешного OAuth2 — генерация JWT, refresh cookie и редирект на фронтенд
`/auth/callback?token=<access>&permissions=<permissions>`.

---

## Модель данных

### Сущности

| Сущность             | Таблица          | Описание                                                |
|----------------------|------------------|---------------------------------------------------------|
| `UserEntity`         | `users`          | Учётная запись: username, email, password, роль, статус |
| `UserProfileEntity`  | `users_profiles` | Расширенный профиль: ФИО, дата рождения, пол, аватар    |
| `RoleEntity`         | `roles`          | Роли с набором прав (many-to-many с permissions)        |
| `PermissionEntity`   | `permissions`    | Отдельные права (имя, описание, категория)              |
| `RefreshTokenEntity` | `refresh_tokens` | Хеши refresh-токенов, устройства, отзыв, ротация        |
| `AuthEventEntity`    | `auth_events`    | Лог auth-событий: входы, выходы, ошибки                 |

Связи: `User → Role (many-to-one) → Permissions (many-to-many)`.
`User ↔ UserProfile (1:1 по UUID)`.

---

## Ролевая модель

| Роль            | Описание                     |
|-----------------|------------------------------|
| `ADMIN`         | Полный доступ                |
| `TUTOR_FULL`    | Преподаватель, полные права  |
| `TUTOR_LIMITED` | Преподаватель, без удаления  |
| `TUTOR_VIEWER`  | Ассистент, только просмотр   |
| `STUDENT_FULL`  | Студент, полные права        |
| `STUDENT_AUDIT` | Слушатель, без сдачи заданий |
| `MODERATOR`     | Модератор контента           |
| `MANAGER`       | Менеджер                     |

**При регистрации через форму:** `STUDENT_FULL` или `TUTOR_LIMITED` (флаг `tutor`).
**При OAuth2:** `STUDENT_FULL`.

---

## Безопасность

### JWT-аутентификация

- **Access токен** — краткосрочный (по умолчанию 5 мин), передаётся в `Authorization: Bearer <token>`
- **Refresh токен** — долгосрочный (7 дней), хранится в `httpOnly; Secure; SameSite=Lax` cookie
- Токены содержат: `userId (sub)`, `username`, `role`, `permissions`
- Refresh токены хранятся в БД в **хешированном** виде с поддержкой ротации и отзыва

### OAuth2 провайдеры

- **Google** — scope: `email`, `profile`
- **Яндекс** — scope: `login:email`, `login:info`, `login:avatar`, `login:birthday`

### CORS

Разрешены запросы только с `${origins.front}` (по умолчанию `http://localhost:3000`).

### Публичные эндпоинты

`/api/auth/**`, `/oauth2/**`, `/login/**`, `/swagger-ui/**`, `/actuator/**`

Все остальные требуют аутентификации. Авторизация на уровне методов — через `@PreAuthorize`.

---

## Миграции БД

| Версия  | Описание                                       |
|---------|------------------------------------------------|
| V1–V4   | Базовая схема: users, users_profiles, индексы  |
| V5–V8   | Ролевая модель: роли, права, назначения        |
| V9      | Аудит: auth_events, refresh_tokens             |
| V10–V13 | Username, external_id (OAuth2), уточнение прав |

Flyway применяется автоматически при старте. `ddl-auto=none`.

---

## Конфигурация

Основные переменные окружения (`.env`):

| Переменная                                  | Описание            | Значение по умолчанию                          |
|---------------------------------------------|---------------------|------------------------------------------------|
| `URL_DB`                                    | JDBC URL PostgreSQL | —                                              |
| `USERNAME_DB` / `PASSWORD_DB`               | Учётка БД           | —                                              |
| `DB_SCHEMA`                                 | Schema PostgreSQL   | `public`                                       |
| `JWT_SECRET_KEY`                            | Секрет JWT (Base64) | `ajscqSVPNj4GNzF+Ln2H6yaE2etWGExa618+TDP96ZE=` |
| `JWT_ACCESS_EXPIRATION_MS`                  | TTL access токена   | `300000` (5 мин)                               |
| `JWT_REFRESH_EXPIRATION_MS`                 | TTL refresh токена  | `604800000` (7 дней)                           |
| `MINIO_URL`                                 | URL MinIO           | `http://localhost:9000`                        |
| `USERNAME_MINIO` / `PASSWORD_MINIO`         | Учётка MinIO        | `minio` / `minio123`                           |
| `BUCKET_NAME`                               | Бакет для аватаров  | `avatar-images`                                |
| `origins.front`                             | URL фронтенда       | `http://localhost:3000`                        |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2       | —                                              |
| `YANDEX_CLIENT_ID` / `YANDEX_CLIENT_SECRET` | Яндекс OAuth2       | —                                              |

---

## Docker

```bash
./gradlew build
docker build -t tutor-auth .
docker run -p 8081:8081 --env-file .env tutor-auth
```

Образ: `eclipse-temurin:25-jdk`, порт `8081`.

---

## Swagger

- **Swagger UI:** http://localhost:8081/swagger-ui/index.html
- **OpenAPI YAML:** http://localhost:8081/teacher-portal/auth-service/api-docs.yaml

---

## Внутренние зависимости

Проект использует три внутренние библиотеки из приватного Maven-репозитория (`razum0ff.ru:9000`):

| Артефакт     | Версия | Предоставляет                                |
|--------------|--------|----------------------------------------------|
| `api-errors` | 1.0.6  | `ErrorCode`, `ApiError`, `PlatformException` |
| `base-utils` | 1.0.0  | `DtoUtils` (safe getter chains)              |
| `jwt`        | 1.0.2  | `JwtUserPrincipal` (principal из JWT)        |
