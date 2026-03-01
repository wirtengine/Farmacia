package com.farmacia.controller;

import com.farmacia.dto.VentaRequestDTO;
import com.farmacia.dto.VentaResponseDTO;
import com.farmacia.model.Usuario;
import com.farmacia.repository.UsuarioRepository;
import com.farmacia.service.VentaService;
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
@RequestMapping("/api/ventas")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private UsuarioRepository usuarioRepository; // Necesario para obtener el usuario por username

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<VentaResponseDTO> crearVenta(@Valid @RequestBody VentaRequestDTO request) {
        VentaResponseDTO venta = ventaService.crearVenta(request);
        return new ResponseEntity<>(venta, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<VentaResponseDTO> obtenerVenta(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerVenta(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VentaResponseDTO>> listarVentas() {
        return ResponseEntity.ok(ventaService.listarVentas());
    }

    @GetMapping("/vendedor/{vendedorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VentaResponseDTO>> listarPorVendedor(@PathVariable Long vendedorId) {
        return ResponseEntity.ok(ventaService.listarVentasPorVendedor(vendedorId));
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<VentaResponseDTO>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(ventaService.listarVentasPorCliente(clienteId));
    }

    // 🔥 NUEVO ENDPOINT: devuelve las ventas del vendedor autenticado
    @GetMapping("/mis-ventas")
    @PreAuthorize("hasRole('VENDEDOR')")
    public ResponseEntity<List<VentaResponseDTO>> listarMisVentas(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        Usuario vendedor = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return ResponseEntity.ok(ventaService.listarVentasPorVendedor(vendedor.getId()));
    }
}