package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllByActivoTrue();

    Optional<Cliente> findByIdAndActivoTrue(Long id);

    Optional<Cliente> findByCedula(String cedula);

    boolean existsByCedula(String cedula);
}