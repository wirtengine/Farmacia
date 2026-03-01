package com.farmacia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class VentaRequestDTO {

    private Long clienteId; // null si es venta rápida

    @NotNull
    private List<DetalleVentaRequestDTO> detalles;

    private BigDecimal descuento = BigDecimal.ZERO; // descuento global a la venta

    // Método para validar que la lista no esté vacía (se puede hacer en el service)
}