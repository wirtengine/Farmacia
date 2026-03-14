package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DevolucionAprobarRequest {
    @NotNull
    private Long devolucionId;

    @NotNull
    private Long aprobadoPorId;

    @NotNull
    private Boolean aprobada;

    private String motivoRechazo; // solo si es rechazada
}