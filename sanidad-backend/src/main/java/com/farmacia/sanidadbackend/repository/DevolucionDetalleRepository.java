package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.DevolucionDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface DevolucionDetalleRepository extends JpaRepository<DevolucionDetalle, Long> {

    @Query("SELECT d.loteDetalle.medicamento.id, d.loteDetalle.medicamento.nombre, SUM(d.cantidadDevuelta) " +
            "FROM DevolucionDetalle d WHERE d.devolucion.estado = 'APROBADA' " +
            "GROUP BY d.loteDetalle.medicamento.id, d.loteDetalle.medicamento.nombre " +
            "ORDER BY SUM(d.cantidadDevuelta) DESC")
    List<Object[]> findTotalDevueltoPorMedicamento();
}