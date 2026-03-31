package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.VentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {

    @Query("SELECT ld.medicamento.nombre, SUM(vd.subtotal) FROM VentaDetalle vd JOIN vd.loteDetalle ld WHERE vd.venta.activo = true GROUP BY ld.medicamento.id, ld.medicamento.nombre ORDER BY SUM(vd.subtotal) DESC")
    List<Object[]> findTopProductosByIngresos();

    @Query("SELECT COUNT(vd) > 0 FROM VentaDetalle vd WHERE vd.loteDetalle.lote.id = :loteId")
    boolean existsByLoteId(@Param("loteId") Long loteId);

    @Query("SELECT ld.medicamento.id, SUM(vd.cantidad) FROM VentaDetalle vd JOIN vd.loteDetalle ld WHERE vd.venta.fecha BETWEEN :desde AND :hasta AND vd.venta.activo = true GROUP BY ld.medicamento.id")
    List<Object[]> sumCantidadByMedicamentoEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("SELECT ld2.medicamento.id, ld2.medicamento.nombre, COUNT(*) as freq " +
            "FROM VentaDetalle vd1 " +
            "JOIN vd1.venta v " +
            "JOIN vd1.loteDetalle ld1 " +
            "JOIN VentaDetalle vd2 ON vd2.venta.id = v.id " +
            "JOIN vd2.loteDetalle ld2 " +
            "WHERE ld1.medicamento.id = :medicamentoId " +
            "AND ld2.medicamento.id != :medicamentoId " +
            "GROUP BY ld2.medicamento.id, ld2.medicamento.nombre " +
            "ORDER BY freq DESC")
    List<Object[]> findComplementaryProducts(@Param("medicamentoId") Long medicamentoId);

    @Query("SELECT ld.medicamento.id, SUM(vd.cantidad) FROM VentaDetalle vd JOIN vd.loteDetalle ld WHERE vd.venta.fecha >= :fechaInicio AND vd.venta.activo = true GROUP BY ld.medicamento.id")
    List<Object[]> sumVentasPorMedicamentoDesde(@Param("fechaInicio") LocalDateTime fechaInicio);
}