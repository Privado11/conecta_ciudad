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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accesses")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Access {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_at")
    @CreationTimestamp
    private LocalDateTime accessAt;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "user_agent", length = 500) 
    private String userAgent;

    @Column(name = "location", length = 100) 
    private String location;

    @Column(name = "success", nullable = false) 
    private Boolean success;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}