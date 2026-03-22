package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RackRequest {
    @NotBlank
    private String nombre;
    private String descripcion;
    @NotNull @Min(1)
    private Integer ancho;
    @NotNull @Min(1)
    private Integer alto;
    @NotNull @Min(1)
    private Integer profundidad;
}