package com.farmacia.repository;

import com.farmacia.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByIdentificacion(String identificacion);
    List<Cliente> findByActivoTrue();
    List<Cliente> findByActivoTrueAndNombreCompletoContainingIgnoreCase(String termino);
    List<Cliente> findByActivoTrueAndIdentificacionContaining(String termino);
}