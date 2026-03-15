package com.farmacia.sanidadbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class VendedorRankingDTO {
    private String username;
    private long cantidadVentas;
    private BigDecimal totalVentas;
}