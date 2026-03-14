package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Lote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findByActivoTrue();

    Optional<Lote> findByIdAndActivoTrue(Long id);

    boolean existsByNumeroLote(String numeroLote);
}