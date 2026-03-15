package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DevolucionProveedorRequest {

    @NotNull
    private Long loteId;

    @NotNull
    private Long solicitadoPorId;

    private String motivo; // opcional

    @NotEmpty
    private List<DevolucionProveedorDetalleRequest> detalles;
}