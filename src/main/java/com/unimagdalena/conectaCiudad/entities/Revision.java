package com.unimagdalena.conectaCiudad.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="revisiones")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Revision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_revision", nullable = true)
    private LocalDateTime fechaRevision;

    @Column(name = "fecha_limite", nullable = false) /*no pueden pasar más de 10 días sin revisar desde su creación*/
    private LocalDateTime fechaLimite;

    @Column(length = 500, nullable = true)
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario curador; //debe ser curador, sino no puede hacer la revisión

    @OneToOne
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;
}
