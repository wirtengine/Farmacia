package com.farmacia.sanidadbackend.inteligencia.alerts;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status);
    List<Alert> findAllByOrderByCreatedAtDesc();
    boolean existsByTypeAndRelatedEntityIdAndStatus(AlertType type, Long entityId, AlertStatus status);
}