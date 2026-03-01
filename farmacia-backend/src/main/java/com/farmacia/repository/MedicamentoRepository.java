package com.farmacia.repository;

import com.farmacia.model.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {
    Optional<Medicamento> findByRegistroSanitario(String registroSanitario);
    List<Medicamento> findByActivoTrue(); // para listar solo activos
}