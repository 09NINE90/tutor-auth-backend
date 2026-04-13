package ru.razumoff.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Set;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "roles")
public class RoleEntity {

    /** Уникальный идентификатор роли */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Уникальное название роли (например, STUDENT_FULL, ADMIN) */
    @Column(nullable = false, unique = true)
    private String name;

    /** Текстовое описание роли */
    @Column(length = 100)
    private String description;

    /** Дата и время создания роли */
    @CreationTimestamp
    private OffsetDateTime createdAt;

    /** Набор прав, связанных с ролью */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> permissions;
}
