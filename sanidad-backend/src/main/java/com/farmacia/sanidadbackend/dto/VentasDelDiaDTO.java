package com.farmacia.sanidadbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class VentasDelDiaDTO {
    private long cantidadVentas;
    private BigDecimal totalVentas;
}