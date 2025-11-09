package com.unimagdalena.conectaCiudad.entities;

import java.time.LocalDateTime;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "projects")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 150, nullable = false)
    private String objectives;

    @Column(length = 250, nullable = false, name = "beneficiary_populations")
    private String beneficiaryPopulations;

    @Column(length = 150, nullable = false)
    private String budgets;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;
}