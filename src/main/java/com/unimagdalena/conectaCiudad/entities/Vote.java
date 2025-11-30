package com.unimagdalena.conectaCiudad.entities;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import com.unimagdalena.conectaCiudad.enums.VoteType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "votes", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vote_user_project", columnNames = {"user_id", "project_id"})
    },
    indexes = {
        @Index(name = "idx_vote_project", columnList = "project_id"),
        @Index(name = "idx_vote_user", columnList = "user_id"),
        @Index(name = "idx_vote_type", columnList = "vote_type"),
        @Index(name = "idx_vote_created_at", columnList = "voted_at DESC")
    }
)
@Immutable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Vote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User voter;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 20)
    private VoteType voteType;
    
    @CreationTimestamp
    @Column(name = "voted_at", nullable = false, updatable = false)
    private OffsetDateTime votedAt;

}
