package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByActivoTrue();

    Optional<Venta> findByIdAndActivoTrue(Long id);

    Optional<Venta> findByNumeroFactura(String numeroFactura);

    boolean existsByNumeroFactura(String numeroFactura);
}