package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.LoteDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface LoteDetalleRepository extends JpaRepository<LoteDetalle, Long> {

    // Para evitar problemas de concurrencia al vender, podemos usar bloqueo pesimista
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ld from LoteDetalle ld where ld.id = :id")
    Optional<LoteDetalle> findByIdWithLock(@Param("id") Long id);
}