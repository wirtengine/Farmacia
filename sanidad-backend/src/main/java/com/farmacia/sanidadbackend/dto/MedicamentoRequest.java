package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MedicamentoRequest {

    @NotBlank(message = "El registro sanitario es obligatorio")
    private String registroSanitario;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "La presentación es obligatoria")
    private String presentacion;

    @NotBlank(message = "La vía es obligatoria")
    private String via;

    private String fabricante;

    private String tipoVenta;

    @NotNull(message = "El precio unitario es obligatorio")
    @Positive(message = "El precio debe ser mayor que cero")
    private BigDecimal precioUnitario;

    private Boolean receta = false;
}