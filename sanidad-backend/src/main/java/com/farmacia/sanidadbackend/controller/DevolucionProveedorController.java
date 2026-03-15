package com.farmacia.sanidadbackend.controller;

import com.farmacia.sanidadbackend.dto.DevolucionProveedorAprobarRequest;
import com.farmacia.sanidadbackend.dto.DevolucionProveedorRequest;
import com.farmacia.sanidadbackend.dto.DevolucionProveedorResponse;
import com.farmacia.sanidadbackend.service.DevolucionProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devoluciones-proveedor")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DevolucionProveedorController {

    private final DevolucionProveedorService devolucionProveedorService;

    @PostMapping("/solicitar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionProveedorResponse> solicitarDevolucion(@Valid @RequestBody DevolucionProveedorRequest request) {
        return new ResponseEntity<>(devolucionProveedorService.solicitarDevolucion(request), HttpStatus.CREATED);
    }

    @PutMapping("/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionProveedorResponse> aprobarDevolucion(@Valid @RequestBody DevolucionProveedorAprobarRequest request) {
        return ResponseEntity.ok(devolucionProveedorService.aprobarDevolucion(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DevolucionProveedorResponse>> listarDevoluciones() {
        return ResponseEntity.ok(devolucionProveedorService.listarDevoluciones());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionProveedorResponse> obtenerDevolucion(@PathVariable Long id) {
        return ResponseEntity.ok(devolucionProveedorService.obtenerDevolucion(id));
    }
}