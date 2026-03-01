package com.farmacia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DetalleVentaRequestDTO {
    @NotNull
    private Long medicamentoId; // Cambiado de loteId a medicamentoId

    @NotNull
    @Positive
    private Integer cantidad;

    private BigDecimal descuento = BigDecimal.ZERO;
}