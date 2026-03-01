package com.farmacia.controller;

import com.farmacia.dto.MedicamentoRequestDTO;
import com.farmacia.dto.MedicamentoResponseDTO;
import com.farmacia.service.MedicamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/medicamentos")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}) // ajusta según necesites
public class MedicamentoController {

    @Autowired
    private MedicamentoService medicamentoService;

    // Crear medicamento (solo admin)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponseDTO> crear(@Valid @RequestBody MedicamentoRequestDTO request) {
        MedicamentoResponseDTO response = medicamentoService.crearMedicamento(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Listar todos los activos (admin y vendedor)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<MedicamentoResponseDTO>> listarActivos() {
        List<MedicamentoResponseDTO> medicamentos = medicamentoService.listarActivos();
        return ResponseEntity.ok(medicamentos);
    }

    // Obtener por id (admin y vendedor)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<MedicamentoResponseDTO> obtenerPorId(@PathVariable Long id) {
        MedicamentoResponseDTO medicamento = medicamentoService.obtenerPorId(id);
        return ResponseEntity.ok(medicamento);
    }

    // Actualizar medicamento (solo admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponseDTO> actualizar(@PathVariable Long id,
                                                             @Valid @RequestBody MedicamentoRequestDTO request) {
        MedicamentoResponseDTO response = medicamentoService.actualizarMedicamento(id, request);
        return ResponseEntity.ok(response);
    }

    // Eliminación lógica (desactivar) solo admin
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        medicamentoService.desactivarMedicamento(id);
        return ResponseEntity.noContent().build();
    }
}