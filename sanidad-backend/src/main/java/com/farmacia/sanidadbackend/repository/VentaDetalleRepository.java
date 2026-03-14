package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.VentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {
}