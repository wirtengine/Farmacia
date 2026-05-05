package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.dto.LotesMedicamentoDTO;
import com.farmacia.sanidadbackend.dto.StockMedicamentoDTO;
import com.farmacia.sanidadbackend.model.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    List<Medicamento> findByActivoTrue();

    Optional<Medicamento> findByIdAndActivoTrue(Long id);

    List<Medicamento> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    boolean existsByRegistroSanitario(String registroSanitario);

    boolean existsByRegistroSanitarioAndActivoTrue(String registroSanitario);

    @Query("SELECT m FROM Medicamento m WHERE m.activo = true AND m.id NOT IN (SELECT DISTINCT ld.medicamento.id FROM LoteDetalle ld JOIN ld.lote l WHERE l.activo = true AND ld IN (SELECT vd.loteDetalle FROM VentaDetalle vd WHERE vd.venta.fecha >= :limite AND vd.venta.activo = true))")
    List<Medicamento> findSinMovimientoDesde(@Param("limite") LocalDateTime limite);

    /**
     * Busca un medicamento por nombre exacto ignorando mayúsculas/minúsculas.
     */
    Optional<Medicamento> findByNombreIgnoreCaseAndActivoTrue(String nombre);

    // ========= SOLO ESTO AGREGASTE =========
    @Query(value = "SELECT * FROM obtener_stock_medicamento(:medicamentoId)", nativeQuery = true)
    StockMedicamentoDTO obtenerStockMedicamento(@Param("medicamentoId") Long medicamentoId);


    /********//// ********/


    @Query(
            value = "SELECT * FROM obtener_lotes_medicamento(:medicamentoId)",
            nativeQuery = true
    )
    LotesMedicamentoDTO obtenerLotesMedicamento(
            @Param("medicamentoId") Long medicamentoId);
}