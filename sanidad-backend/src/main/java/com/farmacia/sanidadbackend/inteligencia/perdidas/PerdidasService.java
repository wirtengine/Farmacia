package com.farmacia.sanidadbackend.inteligencia.perdidas;

import com.farmacia.sanidadbackend.inteligencia.perdidas.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PerdidasService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<ProductoVencidoDTO> obtenerProductosVencidos() {
        String sql = "SELECT lote_id, numero_lote, fecha_vencimiento, medicamento_id, medicamento_nombre, cantidad_vencida, valor_perdido FROM vw_productos_vencidos";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ProductoVencidoDTO dto = new ProductoVencidoDTO();
            dto.setLoteId(rs.getLong("lote_id"));
            dto.setNumeroLote(rs.getString("numero_lote"));
            dto.setFechaVencimiento(rs.getDate("fecha_vencimiento").toLocalDate());
            dto.setMedicamentoId(rs.getLong("medicamento_id"));
            dto.setMedicamentoNombre(rs.getString("medicamento_nombre"));
            dto.setCantidadVencida(rs.getInt("cantidad_vencida"));
            dto.setValorPerdido(rs.getBigDecimal("valor_perdido"));
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public List<ProductoInmovilDTO> obtenerProductosInmoviles() {
        String sql = "SELECT medicamento_id, medicamento_nombre, stock_actual, dias_sin_movimiento, valor_inmovilizado FROM vw_productos_inmoviles";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ProductoInmovilDTO dto = new ProductoInmovilDTO();
            dto.setMedicamentoId(rs.getLong("medicamento_id"));
            dto.setMedicamentoNombre(rs.getString("medicamento_nombre"));
            dto.setStockActual(rs.getInt("stock_actual"));
            dto.setDiasSinMovimiento(rs.getInt("dias_sin_movimiento"));
            dto.setValorInmovilizado(rs.getBigDecimal("valor_inmovilizado"));
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public List<InconsistenciaStockDTO> obtenerInconsistenciasStock() {
        String sql = "SELECT lote_detalle_id, medicamento_id, medicamento_nombre, cantidad_lote, cantidad_ubicaciones, diferencia FROM vw_inconsistencias_stock";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            InconsistenciaStockDTO dto = new InconsistenciaStockDTO();
            dto.setLoteDetalleId(rs.getLong("lote_detalle_id"));
            dto.setMedicamentoId(rs.getLong("medicamento_id"));
            dto.setMedicamentoNombre(rs.getString("medicamento_nombre"));
            dto.setCantidadLote(rs.getInt("cantidad_lote"));
            dto.setCantidadUbicaciones(rs.getInt("cantidad_ubicaciones"));
            dto.setDiferencia(rs.getInt("diferencia"));
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public ResumenPerdidasDTO obtenerResumenPerdidas() {
        String sql = "SELECT total_perdidas_vencimiento, cantidad_productos_vencidos, total_inmovilizado, cantidad_productos_inmoviles, cantidad_inconsistencias FROM vw_resumen_perdidas";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            ResumenPerdidasDTO resumen = new ResumenPerdidasDTO();
            resumen.setTotalPerdidasVencimiento(rs.getBigDecimal("total_perdidas_vencimiento"));
            resumen.setCantidadProductosVencidos(rs.getInt("cantidad_productos_vencidos"));
            resumen.setTotalInmovilizado(rs.getBigDecimal("total_inmovilizado"));
            resumen.setCantidadProductosInmoviles(rs.getInt("cantidad_productos_inmoviles"));
            resumen.setCantidadInconsistencias(rs.getInt("cantidad_inconsistencias"));
            return resumen;
        });
    }
}