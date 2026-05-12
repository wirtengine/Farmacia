package com.farmacia.sanidadbackend.controller;

import com.farmacia.sanidadbackend.dto.RecetaResponse;
import com.farmacia.sanidadbackend.service.RecetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/recetas")
@RequiredArgsConstructor
public class RecetaController {

    private final RecetaService recetaService;

    // Subir receta
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('FARMACEUTICO','ADMIN')")
    public ResponseEntity<RecetaResponse> uploadReceta(
            @RequestParam("file") MultipartFile file,
            @RequestParam("codigoMinsa") String codigoMinsa,
            @RequestParam("farmaceuticoId") Long farmaceuticoId) {

        try {
            RecetaResponse response = recetaService.uploadReceta(file, farmaceuticoId, codigoMinsa);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Validar o rechazar receta
    @PutMapping("/{recetaId}/validar")
    @PreAuthorize("hasAnyRole('FARMACEUTICO','ADMIN')")
    public ResponseEntity<RecetaResponse> validar(
            @PathVariable Long recetaId,
            @RequestParam boolean aprobar,
            @RequestParam(required = false) Long farmaceuticoId) {

        RecetaResponse response = recetaService.validarReceta(recetaId, aprobar, farmaceuticoId);
        return ResponseEntity.ok(response);
    }

    // Obtener una receta por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMACEUTICO','ADMIN')")
    public ResponseEntity<RecetaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(recetaService.obtenerReceta(id));
    }

    // Listar recetas pendientes
    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('FARMACEUTICO','ADMIN')")
    public ResponseEntity<List<RecetaResponse>> listarPendientes() {
        return ResponseEntity.ok(recetaService.listarPendientes());
    }

    // Listar recetas por farmacéutico
    @GetMapping("/farmaceutico/{id}")
    @PreAuthorize("hasAnyRole('FARMACEUTICO','ADMIN')")
    public ResponseEntity<List<RecetaResponse>> listarPorFarmaceutico(@PathVariable Long id) {
        return ResponseEntity.ok(recetaService.listarPorFarmaceutico(id));
    }

    // Recetas disponibles para venta
    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('FARMACEUTICO','ADMIN')")
    public ResponseEntity<List<RecetaResponse>> listarDisponibles() {
        return ResponseEntity.ok(recetaService.listarDisponibles());
    }

    // 🔥 NUEVO: Listar todas las recetas
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FARMACEUTICO')")
    public ResponseEntity<List<RecetaResponse>> listarTodas() {
        return ResponseEntity.ok(recetaService.listarTodas());
    }



}