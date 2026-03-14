package com.farmacia.sanidadbackend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DevolucionDetalleResponse {

    private Long id;
    private Long loteDetalleId;
    private String medicamentoNombre;
    private String loteNumero;
    private Integer cantidadDevuelta;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

}