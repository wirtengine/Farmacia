package com.farmacia.controller;

import com.farmacia.dto.ClienteRequestDTO;
import com.farmacia.dto.ClienteResponseDTO;
import com.farmacia.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    // Listar todos los clientes activos (accesible para admin y vendedor)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<ClienteResponseDTO>> listarActivos() {
        List<ClienteResponseDTO> clientes = clienteService.listarActivos();
        return ResponseEntity.ok(clientes);
    }

    // Buscar clientes por término (accesible para admin y vendedor)
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<ClienteResponseDTO>> buscar(@RequestParam String q) {
        List<ClienteResponseDTO> clientes = clienteService.buscar(q);
        return ResponseEntity.ok(clientes);
    }

    // Obtener un cliente por ID (accesible para admin y vendedor)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<ClienteResponseDTO> obtenerPorId(@PathVariable Long id) {
        ClienteResponseDTO cliente = clienteService.obtenerPorId(id);
        return ResponseEntity.ok(cliente);
    }

    // Crear nuevo cliente (accesible para admin y vendedor)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<ClienteResponseDTO> crear(@Valid @RequestBody ClienteRequestDTO request) {
        ClienteResponseDTO nuevo = clienteService.crearCliente(request);
        return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
    }

    // Actualizar cliente (accesible para admin y vendedor)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<ClienteResponseDTO> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody ClienteRequestDTO request) {
        ClienteResponseDTO actualizado = clienteService.actualizarCliente(id, request);
        return ResponseEntity.ok(actualizado);
    }

    // Desactivar cliente (borrado lógico) (accesible para admin y vendedor)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        clienteService.desactivarCliente(id);
        return ResponseEntity.noContent().build();
    }
}