package ru.razumoff.dao.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.razumoff.dao.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUsername(String username);

    @Query("SELECT u FROM UserEntity u " +
            "LEFT JOIN FETCH u.role r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE u.id = :userId AND u.enabled = true")
    Optional<UserEntity> findByUserIdWithRolesAndPermissions(@Param("userId") UUID userId);

    @Query("SELECT u FROM UserEntity u " +
            "LEFT JOIN FETCH u.role r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE u.email = :email AND u.enabled = true")
    Optional<UserEntity> findByEmailWithRolesAndPermissions(@Param("email") String email);

    @Query("SELECT u FROM UserEntity u " +
            "LEFT JOIN FETCH u.role r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE u.username = :username AND u.enabled = true")
    Optional<UserEntity> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.role r LEFT JOIN FETCH r.permissions " +
            "WHERE u.username = :login OR u.email = :login")
    Optional<UserEntity> findByUsernameOrEmailWithRoleAndPermissions(String login);
}
