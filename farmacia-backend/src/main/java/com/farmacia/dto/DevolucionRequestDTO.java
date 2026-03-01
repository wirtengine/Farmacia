package com.farmacia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DevolucionRequestDTO {

    @NotNull
    private Long ventaId;

    private Long detalleId; // null si es devolución total de la venta

    @NotNull
    @Positive
    private Integer cantidad;

    @NotNull
    private String motivo;
}