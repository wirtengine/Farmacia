package com.farmacia.sanidadbackend.controller;

import com.farmacia.sanidadbackend.dto.DevolucionAprobarRequest;
import com.farmacia.sanidadbackend.dto.DevolucionRequest;
import com.farmacia.sanidadbackend.dto.DevolucionResponse;
import com.farmacia.sanidadbackend.service.DevolucionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devoluciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DevolucionController {

    private final DevolucionService devolucionService;

    @PostMapping("/solicitar")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<DevolucionResponse> solicitarDevolucion(@Valid @RequestBody DevolucionRequest request) {
        return new ResponseEntity<>(devolucionService.solicitarDevolucion(request), HttpStatus.CREATED);
    }

    @PutMapping("/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionResponse> aprobarDevolucion(@Valid @RequestBody DevolucionAprobarRequest request) {
        return ResponseEntity.ok(devolucionService.aprobarDevolucion(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<DevolucionResponse>> listarDevoluciones() {
        return ResponseEntity.ok(devolucionService.listarDevoluciones());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<DevolucionResponse> obtenerDevolucion(@PathVariable Long id) {
        return ResponseEntity.ok(devolucionService.obtenerDevolucion(id));
    }
}