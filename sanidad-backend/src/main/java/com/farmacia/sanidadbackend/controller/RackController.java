package com.farmacia.sanidadbackend.controller;

import com.farmacia.sanidadbackend.dto.RackRequest;
import com.farmacia.sanidadbackend.dto.RackResponse;
import com.farmacia.sanidadbackend.service.RackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/racks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RackController {

    private final RackService rackService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<RackResponse>> listarRacks() {
        return ResponseEntity.ok(rackService.listarRacks());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<RackResponse> obtenerRack(@PathVariable Long id) {
        return ResponseEntity.ok(rackService.obtenerRack(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RackResponse> crearRack(@Valid @RequestBody RackRequest request) {
        return new ResponseEntity<>(rackService.crearRack(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RackResponse> actualizarRack(@PathVariable Long id, @Valid @RequestBody RackRequest request) {
        return ResponseEntity.ok(rackService.actualizarRack(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarRack(@PathVariable Long id) {
        rackService.eliminarRack(id);
        return ResponseEntity.noContent().build();
    }
}