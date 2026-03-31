package com.farmacia.sanidadbackend.inteligencia.perdidas.dto;

import lombok.Data;

@Data
public class InconsistenciaStockDTO {
    private Long loteDetalleId;
    private Long medicamentoId;
    private String medicamentoNombre;
    private Integer cantidadLote;
    private Integer cantidadUbicaciones;
    private Integer diferencia;
}