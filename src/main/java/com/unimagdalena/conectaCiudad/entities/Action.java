package com.unimagdalena.conectaCiudad.entities;

import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "actions", indexes = {
    @Index(name = "idx_action_entity", columnList = "entity_type,entity_id"),
    @Index(name = "idx_action_user", columnList = "user_id"),
    @Index(name = "idx_action_date", columnList = "action_at"),
    @Index(name = "idx_action_type", columnList = "action_type"),
    @Index(name = "idx_action_result", columnList = "result"),
    @Index(name = "idx_composite_user_date", columnList = "user_id,action_at DESC")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 50)
    private EntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ActionResult result = ActionResult.SUCCESS;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "action_at", nullable = false)
    private OffsetDateTime actionAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_id")
    private Access access;

    @PrePersist
    protected void onCreate() {
        if (this.result == null) {
            this.result = ActionResult.SUCCESS;
        }
    }
}
