package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Devolucion;
import com.farmacia.sanidadbackend.model.EstadoDevolucion; // Import necesario
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {


    boolean existsByNumeroDevolucion(String numeroDevolucion);


    List<Devolucion> findTop5ByEstadoOrderByFechaSolicitudDesc(EstadoDevolucion estado);
}