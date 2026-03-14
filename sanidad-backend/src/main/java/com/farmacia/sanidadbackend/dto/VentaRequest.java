package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VentaRequest {

    // Cliente opcional (para venta rápida puede ser null)
    private Long clienteId;

    // Usuario que realiza la venta
    @NotNull
    private Long usuarioId;

    // Detalle de productos vendidos
    @NotEmpty
    private List<VentaDetalleRequest> detalles;

    // Nuevos campos para manejo de pagos
    private BigDecimal montoUsadoSaldo; // si es null se asume 0
    private BigDecimal montoEfectivo;   // si es null se asume pago completo según corresponda
}
