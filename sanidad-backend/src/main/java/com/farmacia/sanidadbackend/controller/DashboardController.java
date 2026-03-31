package com.farmacia.sanidadbackend.controller;

import com.farmacia.sanidadbackend.dto.DashboardResponseDTO;
import com.farmacia.sanidadbackend.model.Usuario;
import com.farmacia.sanidadbackend.security.UserDetailsImpl;
import com.farmacia.sanidadbackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<DashboardResponseDTO> obtenerResumen(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Usuario usuario = userDetails.getUsuario();
        return ResponseEntity.ok(dashboardService.obtenerResumen(usuario));
    }
}