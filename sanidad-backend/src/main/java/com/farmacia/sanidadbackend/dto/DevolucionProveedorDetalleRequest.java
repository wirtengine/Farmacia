package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DevolucionProveedorDetalleRequest {

    @NotNull
    private Long loteDetalleId;

    @NotNull
    @Positive
    private Integer cantidadDevuelta;
}