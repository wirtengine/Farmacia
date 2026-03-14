package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Devolucion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {
    boolean existsByNumeroDevolucion(String numeroDevolucion);
}