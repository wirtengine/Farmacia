package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.LoteDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface LoteDetalleRepository extends JpaRepository<LoteDetalle, Long> {

    @Query("SELECT ld.medicamento.nombre, SUM(ld.cantidad) FROM LoteDetalle ld WHERE ld.lote.activo = true GROUP BY ld.medicamento.id, ld.medicamento.nombre ORDER BY SUM(ld.cantidad) ASC")
    List<Object[]> findProductosBajoStock();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ld from LoteDetalle ld where ld.id = :id")
    Optional<LoteDetalle> findByIdWithLock(@Param("id") Long id);
}