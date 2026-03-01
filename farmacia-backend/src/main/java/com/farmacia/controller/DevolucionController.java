package com.farmacia.controller;

import com.farmacia.dto.DevolucionAprobacionDTO;
import com.farmacia.dto.DevolucionRequestDTO;
import com.farmacia.dto.DevolucionResponseDTO;
import com.farmacia.model.Usuario;
import com.farmacia.repository.UsuarioRepository;
import com.farmacia.service.DevolucionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devoluciones")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DevolucionController {

    @Autowired
    private DevolucionService devolucionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Solicitar devolución (vendedor o admin pueden solicitar)
    @PostMapping("/solicitar")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<DevolucionResponseDTO> solicitar(@Valid @RequestBody DevolucionRequestDTO request) {
        DevolucionResponseDTO response = devolucionService.solicitarDevolucion(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Procesar devolución (solo admin)
    @PostMapping("/procesar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionResponseDTO> procesar(@Valid @RequestBody DevolucionAprobacionDTO request) {
        DevolucionResponseDTO response = devolucionService.procesarDevolucion(request);
        return ResponseEntity.ok(response);
    }

    // Listar todas las devoluciones (solo admin)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DevolucionResponseDTO>> listarTodas() {
        return ResponseEntity.ok(devolucionService.listarTodas());
    }

    // Listar devoluciones pendientes (solo admin)
    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DevolucionResponseDTO>> listarPendientes() {
        return ResponseEntity.ok(devolucionService.listarPendientes());
    }

    // Listar devoluciones del vendedor autenticado
    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasRole('VENDEDOR')")
    public ResponseEntity<List<DevolucionResponseDTO>> listarMisSolicitudes(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        Usuario vendedor = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return ResponseEntity.ok(devolucionService.listarPorVendedor(vendedor.getId()));
    }

    // Listar devoluciones de un vendedor específico (solo admin)
    @GetMapping("/vendedor/{vendedorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DevolucionResponseDTO>> listarPorVendedor(@PathVariable Long vendedorId) {
        return ResponseEntity.ok(devolucionService.listarPorVendedor(vendedorId));
    }

    // Obtener una devolución por ID (accesible para admin y vendedor)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<DevolucionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(devolucionService.obtenerPorId(id));
    }
}