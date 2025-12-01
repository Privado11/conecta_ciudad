package com.unimagdalena.conectaCiudad.entities;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "permissions",
    indexes = @Index(name = "idx_permission_code", columnList = "code", unique = true)
)
@Audited
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode(of = "code")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 120)
    private String description;

    @Column(name = "is_critical", nullable = false)
    private boolean isCritical = false;
}
