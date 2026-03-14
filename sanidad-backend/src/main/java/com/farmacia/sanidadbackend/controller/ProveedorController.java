package com.farmacia.sanidadbackend.controller;

import com.farmacia.sanidadbackend.dto.ProveedorRequest;
import com.farmacia.sanidadbackend.dto.ProveedorResponse;
import com.farmacia.sanidadbackend.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    // Listar proveedores
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<List<ProveedorResponse>> listarProveedores() {
        return ResponseEntity.ok(proveedorService.listarProveedores());
    }

    // Obtener proveedor por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<ProveedorResponse> obtenerProveedor(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.obtenerProveedor(id));
    }

    // Crear proveedor (solo ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProveedorResponse> crearProveedor(
            @Valid @RequestBody ProveedorRequest request) {

        ProveedorResponse response = proveedorService.crearProveedor(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Actualizar proveedor
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProveedorResponse> actualizarProveedor(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorRequest request) {

        return ResponseEntity.ok(
                proveedorService.actualizarProveedor(id, request)
        );
    }

    // Suspender proveedor (soft delete)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> suspenderProveedor(@PathVariable Long id) {

        proveedorService.suspenderProveedor(id);

        return ResponseEntity.noContent().build();
    }
}