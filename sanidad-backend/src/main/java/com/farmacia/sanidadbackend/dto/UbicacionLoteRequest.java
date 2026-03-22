package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UbicacionLoteRequest {
    @NotNull
    private Long loteDetalleId;
    @NotNull
    private Long rackId;
    @NotNull @Min(0)
    private Integer nivel;
    @NotNull @Min(0)
    private Integer columna;
    @NotNull @Min(0)
    private Integer profundidadIndex;
    @NotNull @Min(1)
    private Integer cantidad;
}