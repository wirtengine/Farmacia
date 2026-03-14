package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    List<Medicamento> findByActivoTrue();

    Optional<Medicamento> findByIdAndActivoTrue(Long id);

    List<Medicamento> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    boolean existsByRegistroSanitario(String registroSanitario);

    boolean existsByRegistroSanitarioAndActivoTrue(String registroSanitario);
}