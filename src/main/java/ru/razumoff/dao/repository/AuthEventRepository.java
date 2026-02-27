package ru.razumoff.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.razumoff.dao.entity.AuthEventEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuthEventRepository extends JpaRepository<AuthEventEntity, UUID> {
    List<AuthEventEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<AuthEventEntity> findByEmailOrderByCreatedAtDesc(String email);
    void deleteByCreatedAtBefore(OffsetDateTime date);
}
