package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.DevolucionProveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevolucionProveedorRepository extends JpaRepository<DevolucionProveedor, Long> {
    boolean existsByNumeroDevolucion(String numeroDevolucion);
}