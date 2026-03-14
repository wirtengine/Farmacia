package com.farmacia.sanidadbackend.controller;

import com.farmacia.sanidadbackend.dto.LoteRequest;
import com.farmacia.sanidadbackend.dto.LoteResponse;
import com.farmacia.sanidadbackend.service.LoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<List<LoteResponse>> listarLotes() {
        return ResponseEntity.ok(loteService.listarLotes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<LoteResponse> obtenerLote(@PathVariable Long id) {
        return ResponseEntity.ok(loteService.obtenerLote(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<LoteResponse> crearLote(@Valid @RequestBody LoteRequest request) {
        LoteResponse response = loteService.crearLote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<LoteResponse> actualizarLote(
            @PathVariable Long id,
            @Valid @RequestBody LoteRequest request
    ) {
        return ResponseEntity.ok(loteService.actualizarLote(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> suspenderLote(@PathVariable Long id) {
        loteService.suspenderLote(id);
        return ResponseEntity.noContent().build();
    }
}