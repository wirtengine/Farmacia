package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findByActivoTrue();

    Optional<Lote> findByIdAndActivoTrue(Long id);

    boolean existsByNumeroLote(String numeroLote);

    List<Lote> findByFechaVencimientoBetween(LocalDate desde, LocalDate hasta);

    @Query("SELECT l, SUM(ld.cantidad) as stock FROM Lote l JOIN l.detalles ld WHERE l.activo = true AND l.fechaVencimiento < :fechaActual AND ld.cantidad > 0 GROUP BY l")
    List<Object[]> findLotesVencidosConStock(@Param("fechaActual") LocalDate fechaActual);
}