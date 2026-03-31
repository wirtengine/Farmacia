package com.farmacia.sanidadbackend.inteligencia.alerts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

    @GetMapping
    public List<Alert> getAllAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(@PathVariable Long id) {
        Alert alert = alertRepository.findById(id).orElseThrow();
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alertRepository.save(alert);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<Void> generateAlertsManually() {
        alertService.generarAlertas();
        return ResponseEntity.ok().build();
    }
}