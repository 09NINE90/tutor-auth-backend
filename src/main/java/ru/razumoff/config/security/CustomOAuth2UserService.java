package ru.razumoff.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.razumoff.dao.dto.internal.CustomUserOauthInfo;
import ru.razumoff.dao.entity.RoleEntity;
import ru.razumoff.dao.entity.UserEntity;
import ru.razumoff.dao.entity.UserProfileEntity;
import ru.razumoff.dao.enumz.GenderType;
import ru.razumoff.dao.repository.RoleRepository;
import ru.razumoff.dao.repository.UserProfileRepository;
import ru.razumoff.dao.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.razumoff.Constants.GoogleOauth2.*;
import static ru.razumoff.Constants.YandexOauth2.*;

/**
 * Кастомный сервис OAuth2-аутентификации.
 * <p>
 * Расширяет {@link DefaultOAuth2UserService} для кастомной логики создания/поиска
 * пользователя при входе через Google или Яндекс.
 * </p>
 * <p>
 * При первом входе создаёт {@link UserEntity} и {@link UserProfileEntity} с
 * данными из OAuth2-провайдера. При повторных входах обновляет профиль.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;

    /**
     * Загружает данные пользователя из OAuth2-провайдера.
     * <p>
     * Вызывается Spring Security после успешного обмена кода авторизации на токен.
     * Извлекает атрибуты, создаёт или находит пользователя, формирует список прав.
     * </p>
     *
     * @param userRequest запрос информации о пользователе OAuth2
     * @return {@link OAuth2User} с данными и правами пользователя
     * @throws OAuth2AuthenticationException при ошибках OAuth2
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attrs = oAuth2User.getAttributes();

        String provider = userRequest.getClientRegistration().getRegistrationId();

        CustomUserOauthInfo userOauthInfo = getUserAuthInfo(provider, attrs);
        UserEntity user = getOrCreateUser(userOauthInfo);
        List<GrantedAuthority> authList = getUserAuthority(user);

        String userNameAttribute = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(
                authList,
                attrs,
                userNameAttribute
        );
    }

    /**
     * Создаёт {@link CustomUserOauthInfo} из атрибутов OAuth2-провайдера.
     * <p>
     * Маппинг зависит от провайдера: Google и Яндекс используют разные
     * названия полей для одних и тех же данных.
     * </p>
     *
     * @param provider название провайдера ("google" или "yandex")
     * @param attrs    атрибуты пользователя от OAuth2-провайдера
     * @return {@link CustomUserOauthInfo} с данными пользователя
     */
    private CustomUserOauthInfo getUserAuthInfo(String provider, Map<String, Object> attrs) {
        if ("google".equals(provider)) {
            return CustomUserOauthInfo.builder()
                    .externalId(attrs.get(GOOGLE_ID).toString())
                    .email(attrs.get(GOOGLE_EMAIL).toString())
                    .username(attrs.get(GOOGLE_EMAIL).toString().split("@")[0])
                    .firstName(attrs.get(GOOGLE_FIRST_NAME).toString())
                    .lastName(attrs.getOrDefault(GOOGLE_LAST_NAME, "").toString())
                    .gender(GenderType.UNKNOWN.name())
                    .build();
        } else {
            return CustomUserOauthInfo.builder()
                    .externalId(attrs.get(YA_ID).toString())
                    .email(attrs.get(YA_EMAIL).toString())
                    .username(attrs.get(YA_LOGIN).toString())
                    .firstName(attrs.get(YA_FIRST_NAME).toString())
                    .lastName(attrs.get(YA_LAST_NAME).toString())
                    .gender(attrs.getOrDefault(YA_GENDER, GenderType.UNKNOWN.name()).toString())
                    .build();
        }
    }

    /**
     * Находит существующего пользователя или создаёт нового.
     * <p>
     * При создании нового пользователя:
     * <ul>
     *   <li>Устанавливает роль "STUDENT_FULL" по умолчанию</li>
     *   <li>Создаёт {@link UserProfileEntity} с данными из OAuth2</li>
     *   <li>Преобразует гендер из формата провайдера в {@link GenderType}</li>
     * </ul>
     * </p>
     *
     * @param userOauthInfo данные пользователя из OAuth2
     * @return {@link UserEntity} существующий или новоиспеченный пользователь
     */
    private UserEntity getOrCreateUser(CustomUserOauthInfo userOauthInfo) {
        RoleEntity baseRole = roleRepository.findByName("STUDENT_FULL").orElse(null);

        UserEntity user = userRepository.findByEmail(userOauthInfo.getEmail())
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity();
                    newUser.setEmail(userOauthInfo.getEmail());
                    newUser.setUsername(userOauthInfo.getUsername());
                    newUser.setRole(baseRole);
                    newUser.setExternalId(userOauthInfo.getExternalId());
                    newUser.setEnabled(true);
                    return userRepository.save(newUser);
                });

        GenderType genderType;
        if (userOauthInfo.getGender().equals(YA_GENDER_MALE)) {
            genderType = GenderType.MALE;
        } else if (userOauthInfo.getGender().equals(YA_GENDER_FEMALE)) {
            genderType = GenderType.FEMALE;
        } else {
            genderType = GenderType.UNKNOWN;
        }
        UserProfileEntity userInfo = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfileEntity infoEntity = UserProfileEntity.builder()
                            .userId(user.getId())
                            .firstName(userOauthInfo.getFirstName())
                            .lastName(userOauthInfo.getLastName())
                            .gender(genderType)
                            .build();
                    return userProfileRepository.save(infoEntity);
                });

        return user;
    }

    /**
     * Формирует список прав (authorities) пользователя для Spring Security.
     * <p>
     * Включает:
     * <ul>
     *   <li>Роль с префиксом "ROLE_" (например, ROLE_STUDENT_FULL)</li>
     *   <li>Все индивидуальные permissions пользователя</li>
     * </ul>
     * </p>
     *
     * @param user сущность пользователя
     * @return {@link List} прав для Spring Security
     */
    private List<GrantedAuthority> getUserAuthority(UserEntity user) {
        List<GrantedAuthority> authList = new ArrayList<>();

        if (user.getRole() != null) {
            authList.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
        }

        user.getPermissions().stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authList::add);

        return authList;
    }
}