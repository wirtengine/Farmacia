package com.farmacia.sanidadbackend.controller;

import com.farmacia.sanidadbackend.dto.UbicacionLoteRequest;
import com.farmacia.sanidadbackend.dto.UbicacionLoteResponse;
import com.farmacia.sanidadbackend.service.UbicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ubicaciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UbicacionController {

    private final UbicacionService ubicacionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<UbicacionLoteResponse>> listarTodasUbicaciones() {
        return ResponseEntity.ok(ubicacionService.listarTodasUbicaciones());
    }

    @GetMapping("/rack/{rackId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<UbicacionLoteResponse>> listarUbicacionesPorRack(@PathVariable Long rackId) {
        return ResponseEntity.ok(ubicacionService.listarUbicacionesPorRack(rackId));
    }

    @GetMapping("/lote-detalle/{loteDetalleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<UbicacionLoteResponse>> listarUbicacionesPorLoteDetalle(@PathVariable Long loteDetalleId) {
        return ResponseEntity.ok(ubicacionService.listarUbicacionesPorLoteDetalle(loteDetalleId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<UbicacionLoteResponse> obtenerUbicacion(@PathVariable Long id) {
        return ResponseEntity.ok(ubicacionService.obtenerUbicacion(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UbicacionLoteResponse> asignarUbicacion(@Valid @RequestBody UbicacionLoteRequest request) {
        return new ResponseEntity<>(ubicacionService.asignarUbicacion(request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarUbicacion(@PathVariable Long id) {
        ubicacionService.eliminarUbicacion(id);
        return ResponseEntity.noContent().build();
    }
}