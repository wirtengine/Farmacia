package com.farmacia.repository;

import com.farmacia.model.Lote;
import com.farmacia.model.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    // Buscar lotes activos de un medicamento con medicamento cargado (JOIN FETCH)
    @Query("SELECT l FROM Lote l JOIN FETCH l.medicamento WHERE l.medicamento = :medicamento AND l.activo = true ORDER BY l.fechaVencimiento ASC")
    List<Lote> findByMedicamentoAndActivoTrueWithMedicamento(@Param("medicamento") Medicamento medicamento);

    // Versión sin JOIN FETCH (si se necesita en algún contexto sin cargar el medicamento)
    List<Lote> findByMedicamentoAndActivoTrueOrderByFechaVencimientoAsc(Medicamento medicamento);

    // Buscar lotes próximos a vencer (entre hoy y una fecha límite) con medicamento
    @Query("SELECT l FROM Lote l JOIN FETCH l.medicamento WHERE l.fechaVencimiento BETWEEN :hoy AND :limite AND l.activo = true")
    List<Lote> findByFechaVencimientoBetweenAndActivoTrueWithMedicamento(@Param("hoy") LocalDate hoy, @Param("limite") LocalDate limite);

    // Buscar lotes vencidos (antes de hoy) con medicamento
    @Query("SELECT l FROM Lote l JOIN FETCH l.medicamento WHERE l.fechaVencimiento < :fecha AND l.activo = true")
    List<Lote> findByFechaVencimientoBeforeAndActivoTrueWithMedicamento(@Param("fecha") LocalDate fecha);

    // Buscar por número de lote (debe ser único)
    Optional<Lote> findByNumeroLote(String numeroLote);

    // Para FEFO: obtener lotes con stock > 0 ordenados por vencimiento (cargando medicamento)
    @Query("SELECT l FROM Lote l JOIN FETCH l.medicamento WHERE l.medicamento = :medicamento AND l.activo = true AND l.cantidadActual > :cantidad ORDER BY l.fechaVencimiento ASC")
    List<Lote> findByMedicamentoAndActivoTrueAndCantidadActualGreaterThanWithMedicamento(
            @Param("medicamento") Medicamento medicamento, @Param("cantidad") int cantidad);

    // Contar lotes próximos a vencer (para dashboard)
    @Query("SELECT COUNT(l) FROM Lote l WHERE l.fechaVencimiento BETWEEN :hoy AND :limite AND l.activo = true")
    long countProximosAVencer(@Param("hoy") LocalDate hoy, @Param("limite") LocalDate limite);

    // Obtener todos los lotes activos con medicamento (para listados generales)
    @Query("SELECT l FROM Lote l JOIN FETCH l.medicamento WHERE l.activo = true")
    List<Lote> findAllActivosWithMedicamento();

    // Opcional: obtener todos los lotes (incluyendo inactivos) con medicamento
    @Query("SELECT l FROM Lote l JOIN FETCH l.medicamento")
    List<Lote> findAllWithMedicamento();
}