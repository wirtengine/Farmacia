package com.farmacia.sanidadbackend.inteligencia.recommendations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping
    public List<Recommendation> getAllRecommendations() {
        return recommendationRepository.findAllByOrderByPriorityDescCreatedAtDesc();
    }

    @GetMapping("/pending")
    public List<Recommendation> getPendingRecommendations() {
        return recommendationRepository.findByStatusOrderByPriorityDescCreatedAtDesc(RecommendationStatus.PENDING);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> acceptRecommendation(@PathVariable Long id) {
        Recommendation rec = recommendationRepository.findById(id).orElseThrow();
        rec.setStatus(RecommendationStatus.ACCEPTED);
        rec.setRespondedAt(LocalDateTime.now());
        recommendationRepository.save(rec);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<Void> dismissRecommendation(@PathVariable Long id) {
        Recommendation rec = recommendationRepository.findById(id).orElseThrow();
        rec.setStatus(RecommendationStatus.DISMISSED);
        rec.setRespondedAt(LocalDateTime.now());
        recommendationRepository.save(rec);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<Void> generateRecommendations() {
        recommendationService.generarRecomendaciones();
        return ResponseEntity.ok().build();
    }
}