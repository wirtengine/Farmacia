package com.farmacia.sanidadbackend.dto;

import lombok.Data;

@Data
public class DevolucionProveedorDetalleResponse {

    private Long id;
    private Long loteDetalleId;
    private String medicamentoNombre;
    private String loteNumero;
    private Integer cantidadDevuelta;
}