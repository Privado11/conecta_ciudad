package com.unimagdalena.conectaCiudad.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByStartAt(LocalDateTime startAt);
    List<Review> findByDueAt(LocalDateTime dueAt);
    List<Review> findByReviewedAt(LocalDateTime reviewedAt);
    List<Review> findByCuratorId(Long curatorId);
    List<Review> findByProjectId(Long projectId);
    long countByCuratorIdAndReviewedAtIsNull(Long curatorId);
    long countByCuratorIdAndReviewedAtIsNotNull(Long curatorId);
    long countByCuratorId(Long curatorId);
}