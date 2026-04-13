package ru.razumoff.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.razumoff.jwt.JwtUserPrincipal;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * JWT-фильтр аутентификации.
 * <p>
 * Перехватывает каждый HTTP-запрос, извлекает JWT токен из заголовка
 * {@code Authorization} и устанавливает контекст безопасности Spring Security.
 * </p>
 * <p>
 * Пропускает OAuth2-пути (/login/oauth2/, /oauth2/, /login/), т.к. они
 * обрабатываются отдельно Spring Security OAuth2.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * Основной метод фильтрации запросов.
     * <p>
     * Извлекает токен из заголовка {@code Authorization: Bearer <token>},
     * валидирует его и при успехе создаёт {@link UsernamePasswordAuthenticationToken}
     * с информацией о пользователе, ролях и правах.
     * </p>
     *
     * @param request     HTTP-запрос
     * @param response    HTTP-ответ
     * @param filterChain цепочка фильтров
     * @throws ServletException при ошибках сервлета
     * @throws IOException      при ошибках ввода-вывода
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        boolean isOAuthPath = path.startsWith("/login/oauth2/") ||
                path.startsWith("/oauth2/") ||
                path.startsWith("/login/");

        if (isOAuthPath) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            if (!jwtService.isTokenValid(token)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = jwtService.extractUserId(token);
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);
            Set<String> permissions = jwtService.extractPermissions(token);

            if (userId != null) {
                JwtUserPrincipal principal = new JwtUserPrincipal(
                        userId,
                        username,
                        token,
                        role,
                        permissions
                );

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user: {} with role: {}", username, role);
            }

        } catch (Exception e) {
            log.warn("JWT processing failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
