package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Receta;
import com.farmacia.sanidadbackend.model.EstadoReceta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecetaRepository extends JpaRepository<Receta, Long> {


    List<Receta> findByEstadoOrderByFechaSubidaDesc(EstadoReceta estado);


    List<Receta> findByFarmaceuticoIdOrderByFechaSubidaDesc(Long farmaceuticoId);


    List<Receta> findByEstadoAndVentaIsNull(EstadoReceta estado);


    List<Receta> findAllByOrderByFechaSubidaDesc();
}