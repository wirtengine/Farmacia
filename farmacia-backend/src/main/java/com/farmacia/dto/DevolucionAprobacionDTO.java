package com.farmacia.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DevolucionAprobacionDTO {

    @NotNull
    private Long devolucionId;

    @NotNull
    private String accion; // "APROBAR" o "RECHAZAR"

    private String observacion;
}