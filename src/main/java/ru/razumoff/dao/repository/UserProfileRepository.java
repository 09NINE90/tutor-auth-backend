package ru.razumoff.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.razumoff.dao.entity.UserProfileEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
    Optional<UserProfileEntity> findByUserId(UUID uuid);

    @Query("SELECT p FROM UserProfileEntity p WHERE p.userId IN :userIds")
    List<UserProfileEntity> findByUserIds(@Param("userIds") List<UUID> userIds);
}
