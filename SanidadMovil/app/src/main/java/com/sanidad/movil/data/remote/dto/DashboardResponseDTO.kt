package com.sanidad.movil.data.remote.dto

data class DashboardResponseDTO(
    val ventasDelDia: VentaResumen?,
    val productosMasRentables: List<ProductoIngreso>?,
    val productosBajoStock: List<ProductoStock>?,
    val rankingVendedores: List<VendedorRanking>?,
    val ventasMesActual: Double?,
    val ventasMesAnterior: Double?,
    val productosSinMovimiento: List<ProductoSinMovimiento>?,
    val productosMayorRotacion: List<ProductoRotacion>?,
    val clientesFrecuentes: List<ClienteFrecuente>?
)

data class VentaResumen(val cantidadVentas: Int?, val totalVentas: Double?)
data class ProductoIngreso(val nombre: String, val ingresos: Double)
data class ProductoStock(val nombre: String, val stockTotal: Int)
data class VendedorRanking(val username: String, val cantidadVentas: Int, val totalVentas: Double)
data class ProductoSinMovimiento(val nombre: String, val stockActual: Int, val ultimaFechaVenta: String?, val unidadesVendidas: Int)
data class ProductoRotacion(val nombre: String, val unidadesVendidas: Int, val cantidadVentas: Int, val totalGenerado: Double)
data class ClienteFrecuente(val nombreCliente: String, val cantidadCompras: Int, val totalGastado: Double, val ultimaCompra: String)