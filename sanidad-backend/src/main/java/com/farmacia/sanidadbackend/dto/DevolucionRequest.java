package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DevolucionRequest {
    @NotNull
    private Long ventaId;

    @NotNull
    private Long solicitadoPorId;

    private String motivo;

    @NotEmpty
    private List<DevolucionDetalleRequest> detalles;
}