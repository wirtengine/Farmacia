package com.farmacia.sanidadbackend.dto;

import lombok.Data;

@Data
public class UbicacionLoteResponse {
    private Long id;
    private Long rackId;
    private String rackNombre;
    private Long loteDetalleId;
    private String medicamentoNombre;
    private Integer nivel;
    private Integer columna;
    private Integer profundidadIndex;
    private Integer cantidad;
    private Boolean activo;
}