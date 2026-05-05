package com.farmacia.sanidadbackend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecetaResponse {
    private Long id;
    private String imagenUrl;
    private LocalDateTime fechaSubida;
    private String estado;
    private Long farmaceuticoId;
    private String farmaceuticoUsername;
    private Long ventaId;
}