package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.Usuario;
import com.farmacia.sanidadbackend.model.Rol; // Asegúrate de importar Rol
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Métodos existentes
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);

    // Nuevos métodos por rol
    long countByRol(Rol rol);
    List<Usuario> findByRol(Rol rol);
}