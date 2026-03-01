package com.farmacia.controller;

import com.farmacia.dto.LoteRequestDTO;
import com.farmacia.dto.LoteResponseDTO;
import com.farmacia.service.LoteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class LoteController {

    @Autowired
    private LoteService loteService;

    // Crear lote (solo admin)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoteResponseDTO> crear(@Valid @RequestBody LoteRequestDTO request) {
        LoteResponseDTO nuevo = loteService.crearLote(request);
        return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
    }

    // Listar todos los lotes activos (admin y vendedor)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<LoteResponseDTO>> listarTodos() {
        List<LoteResponseDTO> lotes = loteService.listarTodos();
        return ResponseEntity.ok(lotes);
    }

    // Listar lotes de un medicamento específico
    @GetMapping("/medicamento/{medicamentoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<LoteResponseDTO>> listarPorMedicamento(@PathVariable Long medicamentoId) {
        List<LoteResponseDTO> lotes = loteService.listarPorMedicamento(medicamentoId);
        return ResponseEntity.ok(lotes);
    }

    // Obtener un lote por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<LoteResponseDTO> obtenerPorId(@PathVariable Long id) {
        LoteResponseDTO lote = loteService.obtenerPorId(id);
        return ResponseEntity.ok(lote);
    }

    // Desactivar lote (solo admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        loteService.desactivarLote(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoint para descontar stock (usado en ventas) - solo admin o vendedor
    @PostMapping("/{id}/descontar")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Void> descontarStock(@PathVariable Long id,
                                               @RequestParam Integer cantidad,
                                               @RequestParam String motivo) {
        loteService.descontarStock(id, cantidad, motivo);
        return ResponseEntity.ok().build();
    }
}