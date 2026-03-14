package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DevolucionDetalleRequest {
    @NotNull
    private Long ventaDetalleId;

    @NotNull
    @Positive
    private Integer cantidadDevuelta;
}