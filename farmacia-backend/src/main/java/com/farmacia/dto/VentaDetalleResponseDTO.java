package com.farmacia.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VentaDetalleResponseDTO {
    private Long id;
    private Long loteId;
    private String numeroLote;
    private Long medicamentoId;
    private String medicamentoNombre;
    private String presentacion;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal costoUnitario;
    private BigDecimal descuento;
    private BigDecimal subtotal;
    private BigDecimal ganancia;
}