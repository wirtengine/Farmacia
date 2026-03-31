package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Rack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RackRepository extends JpaRepository<Rack, Long> {


    List<Rack> findByActivoTrue();


    long countByActivoTrue();
}