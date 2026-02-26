package ru.razumoff.service.impl;

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

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new PlatformException(ErrorCode.AUTH_USER_NOT_FOUND));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,
                new HashSet<>()
        );
    }

}