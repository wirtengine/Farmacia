package com.farmacia.repository;

import com.farmacia.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List; // <-- ESTA IMPORTACIÓN FALTABA

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    List<Usuario> findByActivoTrue(); // <-- Ahora sí reconocido
}