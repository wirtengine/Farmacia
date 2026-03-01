package com.farmacia.repository;

import com.farmacia.model.Lote;
import com.farmacia.model.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    // Buscar lotes activos de un medicamento
    List<Lote> findByMedicamentoAndActivoTrueOrderByFechaVencimientoAsc(Medicamento medicamento);

    // Buscar lotes próximos a vencer (entre hoy y una fecha límite)
    List<Lote> findByFechaVencimientoBetweenAndActivoTrue(LocalDate hoy, LocalDate limite);

    // Buscar lotes vencidos (antes de hoy)
    List<Lote> findByFechaVencimientoBeforeAndActivoTrue(LocalDate fecha);

    // Buscar por número de lote (debe ser único)
    Optional<Lote> findByNumeroLote(String numeroLote);

    // Para FEFO: obtener lotes con stock > 0 ordenados por vencimiento
    List<Lote> findByMedicamentoAndActivoTrueAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(
            Medicamento medicamento, int cantidad);

    // Contar lotes próximos a vencer (para dashboard)
    @Query("SELECT COUNT(l) FROM Lote l WHERE l.fechaVencimiento BETWEEN :hoy AND :limite AND l.activo = true")
    long countProximosAVencer(@Param("hoy") LocalDate hoy, @Param("limite") LocalDate limite);
}