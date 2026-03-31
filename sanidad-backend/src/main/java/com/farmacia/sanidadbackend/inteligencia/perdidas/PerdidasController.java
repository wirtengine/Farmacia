package com.farmacia.sanidadbackend.inteligencia.perdidas;

import com.farmacia.sanidadbackend.inteligencia.perdidas.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perdidas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PerdidasController {

    private final PerdidasService perdidasService;

    @GetMapping("/vencidos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductoVencidoDTO>> getProductosVencidos() {
        return ResponseEntity.ok(perdidasService.obtenerProductosVencidos());
    }

    @GetMapping("/inmoviles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductoInmovilDTO>> getProductosInmoviles() {
        return ResponseEntity.ok(perdidasService.obtenerProductosInmoviles());
    }

    @GetMapping("/inconsistencias")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InconsistenciaStockDTO>> getInconsistenciasStock() {
        return ResponseEntity.ok(perdidasService.obtenerInconsistenciasStock());
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResumenPerdidasDTO> getResumenPerdidas() {
        return ResponseEntity.ok(perdidasService.obtenerResumenPerdidas());
    }
}