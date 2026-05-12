package com.sanidad.movil.data.remote.dto

data class DashboardResponseDTO(
    val ventasDelDia: VentasDelDiaDTO,
    val productosMasRentables: List<ProductoRankingDTO>,
    val productosBajoStock: List<ProductoStockDTO>,
    val rankingVendedores: List<VendedorRankingDTO>,
    val ventasMesActual: Double,
    val ventasMesAnterior: Double
)