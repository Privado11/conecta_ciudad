package com.unimagdalena.conectaCiudad.entities;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="usuarios")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String nombre;

    @Column(unique = true, nullable = false)
    private String cc;

    @Column(unique = true, nullable = false)
    private String correo;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(columnDefinition = "boolean default true")
    private Boolean activo;

    @Column(length = 10, nullable = false)
    private String celular;
    
    @Column(name="fecha_creacion", nullable = true)
    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    @ManyToMany
    @JoinTable(name = "usuarios_roles", joinColumns = @JoinColumn(name="usuario_id"),
    inverseJoinColumns = @JoinColumn(name="rol_id"), uniqueConstraints = {@UniqueConstraint(columnNames = {"usuario_id", "rol_id"})})
    List<Rol> roles;
}
