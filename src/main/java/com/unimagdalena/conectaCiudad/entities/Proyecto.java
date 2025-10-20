package com.unimagdalena.conectaCiudad.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="proyectos")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Proyecto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 50, nullable = false)
    private String nombre;

    @Column(length = 150, nullable = false)
    private String objetivos;

    @Column(length = 250, nullable = false, name="poblaciones_beneficiadas")
    private String poblacionesBeneficiadas;

    @Column(length = 150, nullable = false)
    private String presupuestos;

    @Column(name="fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name="fecha_finalizacion", nullable = false)
    private LocalDateTime fechaFinalizacion;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario creador;

    @ManyToOne
    @JoinColumn(name = "estado_id")
    private Estado estado;
}
