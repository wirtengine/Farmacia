package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecetaRequest {
    @NotNull
    private Long farmaceuticoId;
}