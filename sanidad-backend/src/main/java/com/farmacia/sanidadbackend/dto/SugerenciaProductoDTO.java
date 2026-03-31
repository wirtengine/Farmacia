package com.farmacia.sanidadbackend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SugerenciaProductoDTO {
    private Long medicamentoId;
    private String nombre;
    private String presentacion;
    private BigDecimal precioUnitario;
    private String tipoSugerencia; // "FIFO", "COMPLEMENTARIO", "FRECUENTE_CLIENTE"
    private String mensaje;
    private Long loteDetalleId; // Para FIFO (opcional)
    private Integer cantidadSugerida; // Opcional
}