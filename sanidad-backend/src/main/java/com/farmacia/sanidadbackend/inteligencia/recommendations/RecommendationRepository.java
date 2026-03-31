package com.farmacia.sanidadbackend.inteligencia.recommendations;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByStatusOrderByPriorityDescCreatedAtDesc(RecommendationStatus status);
    List<Recommendation> findAllByOrderByPriorityDescCreatedAtDesc();
    boolean existsByTypeAndRelatedEntityIdAndStatus(RecommendationType type, Long entityId, RecommendationStatus status);
}