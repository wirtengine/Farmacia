package com.farmacia.sanidadbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDTO {
    private VentasDelDiaDTO ventasDelDia;
    private List<ProductoRankingDTO> productosMasRentables;  // top 5
    private List<ProductoStockDTO> productosBajoStock;       // top 5 (menor stock)
    private List<VendedorRankingDTO> rankingVendedores;      // todos ordenados
    private BigDecimal ventasMesActual;
    private BigDecimal ventasMesAnterior;
}