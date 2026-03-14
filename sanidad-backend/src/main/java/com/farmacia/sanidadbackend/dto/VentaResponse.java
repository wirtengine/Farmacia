package com.farmacia.sanidadbackend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VentaResponse {

    private Long id;
    private String numeroFactura;
    private LocalDateTime fecha;

    private Long clienteId;
    private String clienteNombre;
    private String clienteCedula;

    private Long usuarioId;
    private String usuarioUsername;

    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;

    private String tipo;

    // 🔹 CAMPOS DE PAGO
    private BigDecimal montoUsadoSaldo;
    private BigDecimal montoEfectivo;
    private BigDecimal cambio;

    private List<VentaDetalleResponse> detalles;
}