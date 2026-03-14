package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VentaDetalleRequest {
    @NotNull
    private Long loteDetalleId; // ID del LoteDetalle específico

    @NotNull
    @Positive
    private Integer cantidad;
}