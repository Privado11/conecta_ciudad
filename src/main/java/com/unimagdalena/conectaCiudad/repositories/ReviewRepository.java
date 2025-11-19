package com.unimagdalena.conectaCiudad.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.unimagdalena.conectaCiudad.entities.Review;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {
    List<Review> findByStartAt(LocalDateTime startAt);
    List<Review> findByDueAt(LocalDateTime dueAt);
    List<Review> findByReviewedAt(LocalDateTime reviewedAt);
    List<Review> findByCuratorId(Long curatorId);
    List<Review> findByProjectId(Long projectId);
    long countByCuratorIdAndReviewedAtIsNull(Long curatorId);
    long countByCuratorIdAndReviewedAtIsNotNull(Long curatorId);
    long countByCuratorId(Long curatorId);
    @Query("""
        SELECT new map(
            r.curator.id as curatorId,
            COUNT(CASE WHEN r.reviewedAt IS NULL THEN 1 END) as active,
            COUNT(CASE WHEN r.reviewedAt IS NOT NULL THEN 1 END) as completed,
            COUNT(r.id) as total
        )
        FROM Review r
        WHERE r.curator.id IN :curatorIds
        GROUP BY r.curator.id
    """)
    List<Map<String, Object>> getCuratorStatsByIds(@Param("curatorIds") List<Long> curatorIds);
    List<Review> findByCuratorIdAndReviewedAtIsNull(Long curatorId);
    List<Review> findByCuratorIdAndReviewedAtIsNotNull(Long curatorId);
    Page<Review> findAll(Specification<Review> spec, Pageable pageable);
}