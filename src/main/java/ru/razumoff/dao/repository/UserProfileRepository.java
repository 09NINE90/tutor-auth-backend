package ru.razumoff.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.razumoff.dao.dto.response.UserSearchRsDto;
import ru.razumoff.dao.entity.UserProfileEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
    Optional<UserProfileEntity> findByUserId(UUID uuid);

    @Query("SELECT p FROM UserProfileEntity p WHERE p.userId IN :userIds")
    List<UserProfileEntity> findByUserIds(@Param("userIds") List<UUID> userIds);

    @Query(value = """
            SELECT u.id as id, u.email as email, p.first_name as firstName,
                   p.last_name as lastName, p.avatar_s3_key as avatarS3Key
            FROM user_service.users u
                JOIN user_service.users_profiles p ON u.id = p.user_id
                        LEFT JOIN courses_service.course_enrollments ce ON u.id = ce.user_id\s
                                AND ce.course_id = ?4
            WHERE (
                    similarity(LOWER(p.first_name || ' ' || p.last_name), LOWER(?1)) > 0.2
                    OR similarity(LOWER(p.first_name), LOWER(?1)) > 0.25
                    OR similarity(LOWER(p.last_name), LOWER(?1)) > 0.25
                    )
                AND u.id != ?3
                AND ce.user_id IS NULL
            ORDER BY GREATEST(
                similarity(LOWER(p.first_name), LOWER(?1)),
                similarity(LOWER(p.last_name), LOWER(?1)),
                similarity(LOWER(p.first_name || ' ' || p.last_name), LOWER(?1))
            ) DESC
            LIMIT ?2
            """, nativeQuery = true)
    List<UserSearchRsDto> searchByFio(String query, int limit, UUID userId, UUID courseId);

    @Query(value = """
            SELECT u.id as id, u.email as email, 
                   p.first_name as firstName, p.last_name as lastName, p.avatar_s3_key as avatarS3Key
            FROM user_service.users u
            JOIN user_service.users_profiles p ON u.id = p.user_id
                        LEFT JOIN courses_service.course_enrollments ce ON u.id = ce.user_id\s
                                AND ce.course_id = :courseId
            WHERE EXISTS (
                SELECT 1 FROM unnest(:patternsArray) AS pat 
                WHERE lower(p.first_name || ' ' || p.last_name) LIKE pat
                   OR lower(p.first_name) LIKE pat 
                   OR lower(p.last_name) LIKE pat
                   OR lower(u.email) LIKE pat
            )
                AND u.id != :userId
                AND ce.user_id IS NULL
                AND u.id NOT IN (
                                SELECT ur.user_id
                                FROM user_service.user_roles ur
                                JOIN user_service.roles r ON ur.role_id = r.id
                                WHERE r.name = 'TUTOR'
                            )
            ORDER BY 
                (SELECT COUNT(*) FROM unnest(:patternsArray) AS pat 
                 WHERE lower(p.first_name || ' ' || p.last_name) LIKE pat
                    OR lower(p.first_name) LIKE pat 
                    OR lower(p.last_name) LIKE pat
                    OR lower(u.email) LIKE pat) DESC,
                CASE WHEN lower(p.first_name || ' ' || p.last_name) LIKE '%' || lower(:query) || '%' THEN 1 ELSE 0 END DESC,
                p.first_name, p.last_name
            LIMIT :limit
            """, nativeQuery = true)
    List<UserSearchRsDto> searchByTrigramPatterns(
            @Param("patternsArray") String[] patternsArray,
            @Param("query") String query,
            @Param("limit") int limit,
            @Param("userId") UUID userId,
            @Param("courseId") UUID courseId
    );


}
