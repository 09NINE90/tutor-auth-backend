package ru.razumoff.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.razumoff.dao.entity.RoleEntity;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(String name);
}