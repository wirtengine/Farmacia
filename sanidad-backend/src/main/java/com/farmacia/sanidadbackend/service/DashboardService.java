package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.*;
import com.farmacia.sanidadbackend.model.Rol;
import com.farmacia.sanidadbackend.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;

@Service
public class DashboardService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public DashboardResponseDTO obtenerResumen(Usuario usuario) {
        boolean esAdmin = usuario.getRol() == Rol.ADMIN;
        Long usuarioId = usuario.getId();

        // Ventas del día (filtradas por usuario si no es admin)
        String sqlVentasDia = esAdmin ?
                "SELECT cantidad_ventas_dia, total_ventas_dia FROM vw_dashboard_resumen" :
                "SELECT COUNT(*), COALESCE(SUM(total), 0) FROM venta WHERE fecha::date = CURRENT_DATE AND usuario_id = ?";
        List<Object[]> ventasDia = esAdmin ?
                jdbcTemplate.query(sqlVentasDia, (rs, rowNum) -> new Object[]{rs.getLong("cantidad_ventas_dia"), rs.getBigDecimal("total_ventas_dia")}) :
                jdbcTemplate.query(sqlVentasDia, new Object[]{usuarioId}, (rs, rowNum) -> new Object[]{rs.getLong(1), rs.getBigDecimal(2)});

        VentasDelDiaDTO ventasDelDia;
        if (ventasDia.isEmpty()) {
            ventasDelDia = new VentasDelDiaDTO(0, BigDecimal.ZERO);
        } else {
            Object[] row = ventasDia.get(0);
            long cantidad = ((Number) row[0]).longValue();
            BigDecimal total = (BigDecimal) row[1];
            ventasDelDia = new VentasDelDiaDTO(cantidad, total);
        }

        // Ventas mes (actual y anterior)
        BigDecimal ventasMesActual, ventasMesAnterior;
        if (esAdmin) {
            String sqlMes = "SELECT ventas_mes_actual, ventas_mes_anterior FROM vw_dashboard_resumen";
            List<Object[]> res = jdbcTemplate.query(sqlMes, (rs, rowNum) -> new Object[]{rs.getBigDecimal("ventas_mes_actual"), rs.getBigDecimal("ventas_mes_anterior")});
            if (!res.isEmpty()) {
                ventasMesActual = (BigDecimal) res.get(0)[0];
                ventasMesAnterior = (BigDecimal) res.get(0)[1];
            } else {
                ventasMesActual = BigDecimal.ZERO;
                ventasMesAnterior = BigDecimal.ZERO;
            }
        } else {
            String sqlMesActual = "SELECT COALESCE(SUM(total),0) FROM venta WHERE fecha >= date_trunc('month', CURRENT_DATE)::timestamp AND fecha <= (date_trunc('month', CURRENT_DATE) + interval '1 month' - interval '1 second') AND usuario_id = ?";
            String sqlMesAnterior = "SELECT COALESCE(SUM(total),0) FROM venta WHERE fecha >= date_trunc('month', CURRENT_DATE - interval '1 month')::timestamp AND fecha <= (date_trunc('month', CURRENT_DATE) - interval '1 second') AND usuario_id = ?";
            ventasMesActual = jdbcTemplate.queryForObject(sqlMesActual, BigDecimal.class, usuarioId);
            ventasMesAnterior = jdbcTemplate.queryForObject(sqlMesAnterior, BigDecimal.class, usuarioId);
        }

        // Productos más rentables
        List<ProductoRankingDTO> productosMasRentables = Collections.emptyList();
        if (esAdmin) {
            String sqlTop = "SELECT nombre, ingresos FROM vw_productos_mas_rentables LIMIT 5";
            productosMasRentables = jdbcTemplate.query(sqlTop, (rs, rowNum) ->
                    new ProductoRankingDTO(rs.getString("nombre"), rs.getBigDecimal("ingresos")));
        }

        // Productos bajo stock
        List<ProductoStockDTO> productosBajoStock = Collections.emptyList();
        if (esAdmin) {
            String sqlStock = "SELECT nombre, stock_total FROM vw_productos_bajo_stock LIMIT 5";
            productosBajoStock = jdbcTemplate.query(sqlStock, (rs, rowNum) ->
                    new ProductoStockDTO(rs.getString("nombre"), rs.getInt("stock_total")));
        }

        // Ranking vendedores
        List<VendedorRankingDTO> rankingVendedores = Collections.emptyList();
        if (esAdmin) {
            String sqlRanking = "SELECT username, cantidad_ventas, total_ventas FROM vw_ranking_vendedores";
            rankingVendedores = jdbcTemplate.query(sqlRanking, (rs, rowNum) ->
                    new VendedorRankingDTO(
                            rs.getString("username"),
                            rs.getLong("cantidad_ventas"),
                            rs.getBigDecimal("total_ventas")
                    ));
        } else {
            String sqlMisVentas = "SELECT COUNT(*), COALESCE(SUM(total), 0) FROM venta WHERE usuario_id = ?";
            List<Object[]> misVentas = jdbcTemplate.query(sqlMisVentas, new Object[]{usuarioId}, (rs, rowNum) -> new Object[]{rs.getLong(1), rs.getBigDecimal(2)});
            if (!misVentas.isEmpty()) {
                rankingVendedores = List.of(new VendedorRankingDTO(usuario.getUsername(), ((Number) misVentas.get(0)[0]).longValue(), (BigDecimal) misVentas.get(0)[1]));
            }
        }

        List<ProductoSinMovimientoDTO> productosSinMovimiento = Collections.emptyList();

        if (esAdmin) {
            String sqlSinMovimiento = """
        SELECT nombre, stock_actual, ultima_fecha_venta, unidades_vendidas
        FROM vw_productos_sin_movimiento
        LIMIT 5
    """;

            productosSinMovimiento = jdbcTemplate.query(sqlSinMovimiento, (rs, rowNum) ->
                    new ProductoSinMovimientoDTO(
                            rs.getString("nombre"),
                            rs.getInt("stock_actual"),
                            rs.getTimestamp("ultima_fecha_venta") != null
                                    ? rs.getTimestamp("ultima_fecha_venta").toLocalDateTime()
                                    : null,
                            rs.getInt("unidades_vendidas")
                    )
            );
        }

        List<ProductoRotacionDTO> productosMayorRotacion = Collections.emptyList();

        if (esAdmin) {
            String sqlRotacion = """
        SELECT nombre, unidades_vendidas, cantidad_ventas, total_generado
        FROM vw_productos_mayor_rotacion
        LIMIT 5
    """;

            productosMayorRotacion = jdbcTemplate.query(sqlRotacion, (rs, rowNum) ->
                    new ProductoRotacionDTO(
                            rs.getString("nombre"),
                            rs.getInt("unidades_vendidas"),
                            rs.getInt("cantidad_ventas"),
                            rs.getBigDecimal("total_generado")
                    )
            );
        }

        List<ClienteFrecuenteDTO> clientesFrecuentes = Collections.emptyList();

        if (esAdmin) {
            String sqlClientes = """
        SELECT nombre_cliente, cantidad_compras, total_gastado, ultima_compra
        FROM vw_clientes_frecuentes
        LIMIT 5
    """;

            clientesFrecuentes = jdbcTemplate.query(sqlClientes, (rs, rowNum) ->
                    new ClienteFrecuenteDTO(
                            rs.getString("nombre_cliente"),
                            rs.getInt("cantidad_compras"),
                            rs.getBigDecimal("total_gastado"),
                            rs.getTimestamp("ultima_compra") != null
                                    ? rs.getTimestamp("ultima_compra").toLocalDateTime()
                                    : null
                    )
            );
        }

        return new DashboardResponseDTO(
                ventasDelDia,
                productosMasRentables,
                productosBajoStock,
                rankingVendedores,
                ventasMesActual != null ? ventasMesActual : BigDecimal.ZERO,
                ventasMesAnterior != null ? ventasMesAnterior : BigDecimal.ZERO,
                productosSinMovimiento,
                productosMayorRotacion,
                clientesFrecuentes
        );
    }
}