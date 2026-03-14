package com.farmacia.sanidadbackend.controller;

import com.farmacia.sanidadbackend.dto.MedicamentoRequest;
import com.farmacia.sanidadbackend.dto.MedicamentoResponse;
import com.farmacia.sanidadbackend.service.MedicamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicamentos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MedicamentoController {

    private final MedicamentoService medicamentoService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponse> crearMedicamento(
            @Valid @RequestBody MedicamentoRequest request) {

        MedicamentoResponse response = medicamentoService.crearMedicamento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<List<MedicamentoResponse>> listarMedicamentos() {

        List<MedicamentoResponse> medicamentos = medicamentoService.listarActivos();
        return ResponseEntity.ok(medicamentos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<MedicamentoResponse> obtenerMedicamento(@PathVariable Long id) {

        MedicamentoResponse response = medicamentoService.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponse> actualizarMedicamento(
            @PathVariable Long id,
            @Valid @RequestBody MedicamentoRequest request) {

        MedicamentoResponse response = medicamentoService.actualizarMedicamento(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivarMedicamento(@PathVariable Long id) {

        medicamentoService.desactivarMedicamento(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activarMedicamento(@PathVariable Long id) {

        medicamentoService.activarMedicamento(id);
        return ResponseEntity.ok().build();
    }
}