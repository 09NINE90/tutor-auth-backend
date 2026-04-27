package ru.razumoff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.razumoff.config.security.JwtService;
import ru.razumoff.dao.dto.request.LoginRequest;
import ru.razumoff.dao.dto.request.RegisterRequest;
import ru.razumoff.dao.dto.response.AuthRsDto;
import ru.razumoff.dao.entity.PermissionEntity;
import ru.razumoff.dao.entity.RoleEntity;
import ru.razumoff.dao.entity.UserEntity;
import ru.razumoff.dao.repository.RoleRepository;
import ru.razumoff.dao.repository.UserRepository;
import ru.razumoff.exceptions.ErrorCode;
import ru.razumoff.exceptions.PlatformException;
import ru.razumoff.service.ICookieService;
import ru.razumoff.service.IUserProfileService;
import ru.razumoff.service.impl.AuthService;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IUserProfileService userProfileService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ICookieService cookieService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldCreateUserAndReturnTokens() {
        RegisterRequest request = RegisterRequest.builder()
                .username("  student1  ")
                .email("student@example.com")
                .password("password1234")
                .firstName("Ivan")
                .lastName("Petrov")
                .birthDate(java.time.LocalDate.of(2000, 1, 1))
                .tutor(false)
                .build();

        UUID savedUserId = UUID.randomUUID();
        RoleEntity role = role("STUDENT_FULL", "PROFILE_READ", "PROFILE_UPDATE");

        when(userRepository.findByUsername("student1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password1234")).thenReturn("encoded-password");
        when(roleRepository.findByName("STUDENT_FULL")).thenReturn(Optional.of(role));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(savedUserId);
            return user;
        });
        doNothing().when(userProfileService).addUserProfile(eq(savedUserId), eq(request));
        when(jwtService.generateAccessToken(eq(savedUserId), eq("student1"), eq("STUDENT_FULL"), anyCollection()))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken(eq(savedUserId), eq("student1"), eq("STUDENT_FULL"), anyCollection()))
                .thenReturn("refresh-token");

        AuthRsDto response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getPermissions()).containsExactlyInAnyOrder("PROFILE_READ", "PROFILE_UPDATE");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("student1");
        assertThat(savedUser.getEmail()).isEqualTo("student@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.getRole()).isSameAs(role);
        assertThat(savedUser.getId()).isEqualTo(savedUserId);

        verify(userProfileService).addUserProfile(savedUserId, request);
        verify(cookieService).addRefreshCookie("refresh-token");
    }

    @Test
    void registerShouldFailWhenUsernameAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .username("student1")
                .email("student@example.com")
                .password("password1234")
                .firstName("Ivan")
                .lastName("Petrov")
                .birthDate(java.time.LocalDate.of(2000, 1, 1))
                .tutor(false)
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(new UserEntity()));

        PlatformException exception = assertThrows(PlatformException.class, () -> authService.register(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_USER_EXISTS);
        verify(userRepository).findByUsername("student1");
        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(roleRepository, userProfileService, cookieService, passwordEncoder, jwtService);
    }

    @Test
    void registerShouldFailWhenRoleIsMissing() {
        RegisterRequest request = RegisterRequest.builder()
                .username("student2")
                .email("student2@example.com")
                .password("password1234")
                .firstName("Ivan")
                .lastName("Petrov")
                .birthDate(java.time.LocalDate.of(2000, 1, 1))
                .tutor(false)
                .build();

        when(userRepository.findByUsername("student2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("student2@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password1234")).thenReturn("encoded-password");
        when(roleRepository.findByName("STUDENT_FULL")).thenReturn(Optional.empty());

        PlatformException exception = assertThrows(PlatformException.class, () -> authService.register(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROLE_NOT_FOUND);
        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(userProfileService, cookieService, jwtService);
    }

    @Test
    void loginShouldReturnTokensForValidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .login("teacher1")
                .password("password1234")
                .build();

        UUID userId = UUID.randomUUID();
        UserEntity user = userWithRole(userId, "teacher1", "teacher1@example.com",
                "TUTOR_LIMITED", "COURSE_READ", "COURSE_WRITE");

        when(userRepository.findByEmailWithRolesAndPermissions("teacher1")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameWithRolesAndPermissions("teacher1")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(jwtService.generateAccessToken(eq(userId), eq("teacher1"), eq("TUTOR_LIMITED"), anyCollection()))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken(eq(userId), eq("teacher1"), eq("TUTOR_LIMITED"), anyCollection()))
                .thenReturn("refresh-token");

        AuthRsDto response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getPermissions()).containsExactlyInAnyOrder("COURSE_READ", "COURSE_WRITE");

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getPrincipal()).isEqualTo("teacher1");
        assertThat(tokenCaptor.getValue().getCredentials()).isEqualTo("password1234");
        verify(cookieService).addRefreshCookie("refresh-token");
    }

    @Test
    void loginShouldFailWhenPasswordIsInvalid() {
        LoginRequest request = LoginRequest.builder()
                .login("teacher1")
                .password("password1234")
                .build();

        UserEntity user = userWithRole(UUID.randomUUID(), "teacher1", "teacher1@example.com",
                "TUTOR_LIMITED", "COURSE_READ");

        when(userRepository.findByEmailWithRolesAndPermissions("teacher1")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameWithRolesAndPermissions("teacher1")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        PlatformException exception = assertThrows(PlatformException.class, () -> authService.login(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
        verify(cookieService, never()).addRefreshCookie(anyString());
        verifyNoInteractions(jwtService);
    }

    @Test
    void refreshShouldRotateTokensForValidRefreshToken() {
        String refreshToken = "refresh-token";
        UUID userId = UUID.randomUUID();
        UserEntity user = userWithRole(userId, "teacher1", "teacher1@example.com",
                "TUTOR_LIMITED", "COURSE_READ");

        when(jwtService.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtService.extractUserId(refreshToken)).thenReturn(userId);
        when(userRepository.findByUserIdWithRolesAndPermissions(userId)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(eq(userId), eq("teacher1"), eq("TUTOR_LIMITED"), anyCollection()))
                .thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(eq(userId), eq("teacher1"), eq("TUTOR_LIMITED"), anyCollection()))
                .thenReturn("new-refresh-token");

        AuthRsDto response = authService.refresh(refreshToken);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getPermissions()).containsExactly("COURSE_READ");
        verify(cookieService).addRefreshCookie("new-refresh-token");
        verify(cookieService, never()).deleteRefreshCookie();
    }

    @Test
    void refreshShouldFailWhenRefreshTokenIsInvalid() {
        String refreshToken = "refresh-token";

        when(jwtService.isTokenValid(refreshToken)).thenReturn(false);

        PlatformException exception = assertThrows(PlatformException.class, () -> authService.refresh(refreshToken));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_INVALID);
        verify(jwtService).isTokenValid(refreshToken);
        verify(cookieService, times(2)).deleteRefreshCookie();
        verifyNoInteractions(userRepository);
    }

    @Test
    void logoutShouldDeleteRefreshCookie() {
        authService.logout("refresh-token");

        verify(cookieService).deleteRefreshCookie();
    }

    private RoleEntity role(String roleName, String... permissionNames) {
        Set<PermissionEntity> permissions = Arrays.stream(permissionNames)
                .map(name -> new PermissionEntity(null, name, null, null, null))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new RoleEntity(null, roleName, null, null, permissions);
    }

    private UserEntity userWithRole(UUID id, String username, String email, String roleName, String... permissionNames) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);
        user.setRole(role(roleName, permissionNames));
        return user;
    }
}
