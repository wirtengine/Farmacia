package com.farmacia.sanidadbackend.dto;

import lombok.Data;
import java.math.BigDecimal; // <-- Importar

@Data
public class LoteDetalleResponse {
    private Long id;
    private Long medicamentoId;
    private String medicamentoNombre;
    private String medicamentoPresentacion;
    private String fabricante;
    private Integer cantidad;
    private BigDecimal precioUnitario; // <-- NUEVO
}