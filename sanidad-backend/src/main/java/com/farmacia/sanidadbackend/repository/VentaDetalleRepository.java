package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.VentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {

    @Query("SELECT ld.medicamento.nombre, SUM(vd.subtotal) FROM VentaDetalle vd JOIN vd.loteDetalle ld WHERE vd.venta.activo = true GROUP BY ld.medicamento.id, ld.medicamento.nombre ORDER BY SUM(vd.subtotal) DESC")
    List<Object[]> findTopProductosByIngresos();

    @Query("SELECT COUNT(vd) > 0 FROM VentaDetalle vd WHERE vd.loteDetalle.lote.id = :loteId")
    boolean existsByLoteId(@Param("loteId") Long loteId);
}