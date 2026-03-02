package com.farmacia.controller;

import com.farmacia.dto.DevolucionAprobacionDTO;
import com.farmacia.dto.DevolucionRequestDTO;
import com.farmacia.dto.DevolucionResponseDTO;
import com.farmacia.model.Devolucion;
import com.farmacia.model.Usuario;
import com.farmacia.repository.UsuarioRepository;
import com.farmacia.service.DevolucionService;
import com.farmacia.service.DevolucionPdfService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devoluciones")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DevolucionController {

    private static final Logger log = LoggerFactory.getLogger(DevolucionController.class);

    @Autowired
    private DevolucionService devolucionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DevolucionPdfService devolucionPdfService;

    @PostMapping("/solicitar")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<DevolucionResponseDTO> solicitar(
            @Valid @RequestBody DevolucionRequestDTO request) {

        DevolucionResponseDTO response =
                devolucionService.solicitarDevolucion(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/procesar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionResponseDTO> procesar(
            @Valid @RequestBody DevolucionAprobacionDTO request) {

        DevolucionResponseDTO response =
                devolucionService.procesarDevolucion(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DevolucionResponseDTO>> listarTodas() {
        return ResponseEntity.ok(devolucionService.listarTodas());
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DevolucionResponseDTO>> listarPendientes() {
        return ResponseEntity.ok(devolucionService.listarPendientes());
    }

    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasRole('VENDEDOR')")
    public ResponseEntity<List<DevolucionResponseDTO>> listarMisSolicitudes(
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();

        Usuario vendedor = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return ResponseEntity.ok(
                devolucionService.listarPorVendedor(vendedor.getId()));
    }

    @GetMapping("/vendedor/{vendedorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DevolucionResponseDTO>> listarPorVendedor(
            @PathVariable Long vendedorId) {

        return ResponseEntity.ok(
                devolucionService.listarPorVendedor(vendedorId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<DevolucionResponseDTO> obtenerPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                devolucionService.obtenerPorId(id));
    }

    // ✅ Método corregido: usa obtenerDevolucionParaPdf
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<byte[]> generarPdf(@PathVariable Long id) {
        try {

            Devolucion devolucion =
                    devolucionService.obtenerDevolucionParaPdf(id);

            byte[] pdf =
                    devolucionPdfService.generarPdfDevolucion(devolucion);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.inline()
                            .filename("devolucion-" + id + ".pdf")
                            .build()
            );

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error al generar PDF de devolución con id: {}", id, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}