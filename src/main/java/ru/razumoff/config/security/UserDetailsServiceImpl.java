package ru.razumoff.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.razumoff.exceptions.ErrorCode;
import ru.razumoff.exceptions.PlatformException;
import ru.razumoff.dao.entity.UserEntity;
import ru.razumoff.dao.repository.UserRepository;

import java.util.HashSet;

/**
 * Реализация {@link UserDetailsService} для загрузки пользователя из БД.
 * <p>
 * Используется при аутентификации по логину/паролю.
 * Ищет пользователя по username или email и возвращает {@link UserDetails}
 * для Spring Security.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Загружает пользователя по логину (username или email).
     * <p>
     * Сначала ищет по username, затем по email.
     * Если пользователь не найден, выбрасывается {@link PlatformException}.
     * </p>
     * <p>
     * Возвращает {@link org.springframework.security.core.userdetails.User} с
     * пустыми authorities, т.к. роли и права подгружаются отдельно через JWT.
     * </p>
     *
     * @param login username или email пользователя
     * @return {@link UserDetails} для Spring Security
     * @throws UsernameNotFoundException если пользователь не найден
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsernameWithRolesAndPermissions(login)
                .or(() -> userRepository.findByEmailWithRolesAndPermissions(login))
                .orElseThrow(() -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,
                new HashSet<>()
        );
    }

}