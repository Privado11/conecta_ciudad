package com.unimagdalena.conectaCiudad.entities;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_status_history", indexes = {
    @Index(name = "idx_status_history_project", columnList = "project_id"),
    @Index(name = "idx_status_history_changed_at", columnList = "changed_at DESC"),
    @Index(name = "idx_status_history_to_status", columnList = "to_status")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ProjectStatusHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 30)
    private ProjectStatus fromStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private ProjectStatus toStatus;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedBy;
    
    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;
}
