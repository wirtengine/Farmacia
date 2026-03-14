package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findByActivoTrue();

    Optional<Proveedor> findByIdAndActivoTrue(Long id);

    boolean existsByRuc(String ruc);

}