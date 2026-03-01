package com.farmacia.repository;

import com.farmacia.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // Buscar solo proveedores activos (para listados)
    List<Proveedor> findByActivoTrue();

    // Buscar por RUC (para validar duplicados)
    Optional<Proveedor> findByRuc(String ruc);

    // Búsqueda por nombre o RUC (para filtros)
    List<Proveedor> findByNombreContainingIgnoreCaseOrRucContainingIgnoreCaseAndActivoTrue(String nombre, String ruc);
}