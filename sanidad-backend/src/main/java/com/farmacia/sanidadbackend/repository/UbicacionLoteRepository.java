package com.farmacia.sanidadbackend.repository;

import com.farmacia.sanidadbackend.model.UbicacionLote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UbicacionLoteRepository extends JpaRepository<UbicacionLote, Long> {

    List<UbicacionLote> findByRackIdAndActivoTrue(Long rackId);

    List<UbicacionLote> findByLoteDetalleIdAndActivoTrue(Long loteDetalleId);

    @Query("SELECT u FROM UbicacionLote u WHERE u.rack.id = :rackId AND u.nivel = :nivel AND u.columna = :columna AND u.profundidadIndex = :profundidadIndex AND u.activo = true")
    Optional<UbicacionLote> findByCoordenadas(@Param("rackId") Long rackId,
                                              @Param("nivel") Integer nivel,
                                              @Param("columna") Integer columna,
                                              @Param("profundidadIndex") Integer profundidadIndex);

    List<UbicacionLote> findByActivoTrue();
}