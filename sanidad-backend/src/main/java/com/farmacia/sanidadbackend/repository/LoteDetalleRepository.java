package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.LoteDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoteDetalleRepository extends JpaRepository<LoteDetalle, Long> {

    // ⚠️ Este método ya no se usa (reemplazado por vw_productos_bajo_stock en DashboardService)
    @Query("SELECT ld.medicamento.nombre, SUM(ld.cantidad) FROM LoteDetalle ld WHERE ld.lote.activo = true GROUP BY ld.medicamento.id, ld.medicamento.nombre ORDER BY SUM(ld.cantidad) ASC")
    List<Object[]> findProductosBajoStock();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ld from LoteDetalle ld where ld.id = :id")
    Optional<LoteDetalle> findByIdWithLock(@Param("id") Long id);

    // ⚠️ Reemplazado por vw_stock_actual_por_medicamento (AlertService, RecommendationService, PerdidasService)
    @Query("SELECT ld.medicamento.id, SUM(ld.cantidad) FROM LoteDetalle ld WHERE ld.lote.activo = true AND ld.lote.fechaVencimiento > :fechaActual GROUP BY ld.medicamento.id")
    List<Object[]> findStockActualPorMedicamento(@Param("fechaActual") LocalDate fechaActual);

    @Query("SELECT ld FROM LoteDetalle ld WHERE ld.medicamento.id = :medicamentoId AND ld.lote.activo = true AND ld.lote.fechaVencimiento > :fechaActual AND ld.cantidad > 0 ORDER BY ld.lote.fechaVencimiento ASC")
    List<LoteDetalle> findLotesDisponiblesPorMedicamentoOrderByVencimiento(
            @Param("medicamentoId") Long medicamentoId,
            @Param("fechaActual") LocalDate fechaActual
    );

    // ================== NUEVOS MÉTODOS PARA FUNCTION CALLING ==================

    @Query("SELECT m.nombre, SUM(ld.cantidad) FROM LoteDetalle ld JOIN ld.medicamento m WHERE ld.lote.activo = true GROUP BY m.id HAVING SUM(ld.cantidad) < :umbral")
    List<Object[]> findProductosBajoStockConUmbral(@Param("umbral") int umbral);

    @Query("SELECT COALESCE(SUM(ld.cantidad), 0) FROM LoteDetalle ld WHERE ld.medicamento.id = :medicamentoId AND ld.lote.activo = true")
    int sumStockByMedicamento(@Param("medicamentoId") Long medicamentoId);

    @Query("SELECT m.id, m.nombre, COALESCE(SUM(ld.cantidad), 0) as stock, COALESCE(SUM(vd.cantidad), 0) as ventas " +
            "FROM Medicamento m " +
            "LEFT JOIN LoteDetalle ld ON ld.medicamento.id = m.id AND ld.lote.activo = true " +
            "LEFT JOIN VentaDetalle vd ON vd.loteDetalle.id = ld.id AND vd.venta.fecha BETWEEN :inicio AND :fin AND vd.venta.activo = true " +
            "WHERE m.activo = true " +
            "GROUP BY m.id")
    List<Object[]> sugerirReorden(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}