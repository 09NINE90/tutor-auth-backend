package ru.razumoff.config.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;
import ru.razumoff.dao.entity.UserEntity;
import ru.razumoff.dao.repository.UserRepository;
import ru.razumoff.service.ICookieService;

import java.util.List;
import java.util.Map;

import static ru.razumoff.Constants.GoogleOauth2.GOOGLE_EMAIL;
import static ru.razumoff.Constants.YandexOauth2.YA_EMAIL;

/**
 * Основной класс конфигурации безопасности приложения.
 * <p>
 * Настраивает Spring Security для работы с JWT-аутентификацией и OAuth2 провайдерами
 * (Google и Яндекс). Определяет политики доступа, CORS, кодирование паролей и
 * обработчики успешной OAuth2-аутентификации.
 * </p>
 *
 * <p>Использует stateless-сессию, что означает отсутствие хранения сессии на сервере.</p>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final UserRepository userRepository;
    private final ICookieService cookieService;

    /**
     * URL фронтенд-приложения для CORS и редиректов.
     * Injected из application.yml через ${origins.front}
     */
    @Value("${origins.front}")
    private String FRONT_ORIGIN;

    /**
     * Создаёт бин для кодирования паролей с использованием алгоритма BCrypt.
     *
     * @return экземпляр {@link BCryptPasswordEncoder} для хеширования паролей
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Основная цепочка фильтров безопасности.
     * <p>
     * Настраивает:
     * <ul>
     *   <li>Отключение CSRF (т.к. используется JWT, а не сессии)</li>
     *   <li>CORS для фронтенд-приложения</li>
     *   <li>Stateless-сессию (без хранения состояния на сервере)</li>
     *   <li>Обработчики ошибок аутентификации (401) и доступа (403)</li>
     *   <li>Публичные эндпоинты (аутентификация, OAuth2, Swagger, actuator)</li>
     *   <li>OAuth2 login с кастомным обработчиком успеха</li>
     *   <li>JWT-фильтр перед стандартной аутентификацией по логину/паролю</li>
     * </ul>
     *
     * @param http конфигурация {@link HttpSecurity}
     * @return настроенная {@link SecurityFilterChain}
     * @throws Exception при ошибках конфигурации
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((req, res, ex) -> res.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) -> res.setStatus(HttpServletResponse.SC_FORBIDDEN))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/oauth2/**",
                                "/login/**",
                                "/login/oauth2/**",
                                "/teacher-portal/auth-service/api-docs/**",
                                "/teacher-portal/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/actuator/**"
                        ).permitAll()
                        .requestMatchers("/api/user/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(info -> info.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler())
                        .failureHandler((request, response, exception) -> {
                            log.error("Error: {}", exception.getMessage());
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Обработчик успешной OAuth2-аутентификации.
     * <p>
     * После успешного входа через OAuth2 (Google/Яндекс):
     * <ol>
     *   <li>Извлекает email пользователя из атрибутов OAuth2</li>
     *   <li>Находит пользователя в БД</li>
     *   <li>Генерирует JWT access и refresh токены</li>
     *   <li>Устанавливает refresh токен в cookie</li>
     *   <li>Редиректит на фронтенд с access токеном и правами в query-параметрах</li>
     * </ol>
     *
     * @return {@link AuthenticationSuccessHandler} для обработки успеха OAuth2
     */
    @Bean
    public AuthenticationSuccessHandler oAuth2SuccessHandler() {
        return (request, response, authentication) -> {
            DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
            Map<String, Object> attrs = principal.getAttributes();

            String email = attrs.containsKey(GOOGLE_EMAIL)
                    ? attrs.get(GOOGLE_EMAIL).toString()
                    : attrs.get(YA_EMAIL).toString();

            UserEntity user = userRepository.findByUsernameOrEmailWithRoleAndPermissions(email)
                    .orElseThrow(() -> new OAuth2AuthenticationException("User not found after OAuth"));

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            cookieService.addRefreshCookie(refreshToken);

            String targetUrl = UriComponentsBuilder.fromUriString(FRONT_ORIGIN + "/auth/callback")
                    .queryParam("token", accessToken)
                    .queryParam("permissions", user.getPermissions().toString())
                    .build()
                    .toUriString();

            log.info("Redirecting to {}", targetUrl);
            response.sendRedirect(targetUrl);
        };
    }

    /**
     * Источник конфигурации CORS.
     * <p>
     * Разрешает запросы только с указанного фронтенд-URL,
     * поддерживает методы GET, POST, PUT, DELETE, OPTIONS и
     * заголовки Authorization, Content-Type.
     * </p>
     *
     * @return {@link CorsConfigurationSource} для обработки CORS-запросов
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(FRONT_ORIGIN));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Провайдер аутентификации для логина/пароля.
     * <p>
     * Использует {@link UserDetailsService} для загрузки пользователя и
     * {@link BCryptPasswordEncoder} для проверки пароля.
     * </p>
     *
     * @return {@link DaoAuthenticationProvider} для аутентификации
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Менеджер аутентификации.
     * <p>
     * Делегирует создание {@link AuthenticationManager} в Spring Security.
     * Используется для программного вызова аутентификации (например, при login/password flow).
     * </p>
     *
     * @param config конфигурация {@link AuthenticationConfiguration}
     * @return {@link AuthenticationManager} для выполнения аутентификации
     * @throws Exception при ошибках получения менеджера
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
