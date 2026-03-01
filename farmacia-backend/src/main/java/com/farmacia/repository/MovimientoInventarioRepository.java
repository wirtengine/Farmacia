package com.farmacia.repository;

import com.farmacia.model.Lote;
import com.farmacia.model.Medicamento;
import com.farmacia.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByLoteOrderByFechaDesc(Lote lote);

    List<MovimientoInventario> findByMedicamentoOrderByFechaDesc(Medicamento medicamento);

    List<MovimientoInventario> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);
}