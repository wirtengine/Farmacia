package com.farmacia.sanidadbackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VentaDetalleResponse {
    private Long id;
    private Long loteDetalleId;
    private String medicamentoNombre;
    private String presentacion;
    private String loteNumero;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}