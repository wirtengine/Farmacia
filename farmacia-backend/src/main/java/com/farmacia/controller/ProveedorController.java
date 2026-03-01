package com.farmacia.controller;

import com.farmacia.dto.ProveedorRequestDTO;
import com.farmacia.dto.ProveedorResponseDTO;
import com.farmacia.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    // Listar todos los proveedores activos (accesible para admin y vendedor)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<ProveedorResponseDTO>> listarActivos() {
        List<ProveedorResponseDTO> proveedores = proveedorService.listarActivos();
        return ResponseEntity.ok(proveedores);
    }

    // Buscar proveedores por término (accesible para admin y vendedor)
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<ProveedorResponseDTO>> buscar(@RequestParam String q) {
        List<ProveedorResponseDTO> proveedores = proveedorService.buscar(q);
        return ResponseEntity.ok(proveedores);
    }

    // Obtener un proveedor por ID (accesible para admin y vendedor)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<ProveedorResponseDTO> obtenerPorId(@PathVariable Long id) {
        ProveedorResponseDTO proveedor = proveedorService.obtenerPorId(id);
        return ResponseEntity.ok(proveedor);
    }

    // Crear nuevo proveedor (solo admin)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProveedorResponseDTO> crear(@Valid @RequestBody ProveedorRequestDTO request) {
        ProveedorResponseDTO nuevo = proveedorService.crearProveedor(request);
        return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
    }

    // Actualizar proveedor (solo admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProveedorResponseDTO> actualizar(@PathVariable Long id,
                                                           @Valid @RequestBody ProveedorRequestDTO request) {
        ProveedorResponseDTO actualizado = proveedorService.actualizarProveedor(id, request);
        return ResponseEntity.ok(actualizado);
    }

    // Desactivar proveedor (solo admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        proveedorService.desactivarProveedor(id);
        return ResponseEntity.noContent().build();
    }
}