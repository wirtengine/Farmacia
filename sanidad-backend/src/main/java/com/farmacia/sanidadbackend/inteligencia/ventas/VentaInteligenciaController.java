package com.farmacia.sanidadbackend.inteligencia.ventas;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas/inteligencia")
@CrossOrigin(origins = "*")
public class VentaInteligenciaController {

    @Autowired
    private VentaInteligenciaService ventaInteligenciaService;

    @GetMapping("/fifo")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> getLoteFIFO(@RequestParam Long medicamentoId) {
        return ResponseEntity.ok(ventaInteligenciaService.obtenerLoteFIFO(medicamentoId));
    }

    @GetMapping("/complementarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> getComplementarios(@RequestParam Long medicamentoId) {
        return ResponseEntity.ok(ventaInteligenciaService.sugerirComplementarios(medicamentoId));
    }

    @GetMapping("/contexto-cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> getContextoCliente(@RequestParam Long clienteId) {
        return ResponseEntity.ok(ventaInteligenciaService.obtenerContextoCliente(clienteId));
    }

    @GetMapping("/venta-guiada")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> getVentaGuiada(@RequestParam(required = false) Long clienteId,
                                            @RequestParam(required = false) Long medicamentoId) {
        return ResponseEntity.ok(ventaInteligenciaService.obtenerInfoVentaGuiada(clienteId, medicamentoId));
    }
}