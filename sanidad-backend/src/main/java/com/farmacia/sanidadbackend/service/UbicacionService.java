package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.UbicacionLoteRequest;
import com.farmacia.sanidadbackend.dto.UbicacionLoteResponse;
import com.farmacia.sanidadbackend.model.UbicacionLote;
import com.farmacia.sanidadbackend.repository.LoteDetalleRepository;
import com.farmacia.sanidadbackend.repository.RackRepository;
import com.farmacia.sanidadbackend.repository.UbicacionLoteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UbicacionService {

    private final JdbcTemplate jdbcTemplate;
    private final UbicacionLoteRepository ubicacionRepository;
    private final RackRepository rackRepository;
    private final LoteDetalleRepository loteDetalleRepository;

    public List<UbicacionLoteResponse> listarTodasUbicaciones() {
        return ubicacionRepository.findByActivoTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Asigna una ubicación usando la función fn_asignar_ubicacion.
     */
    @Transactional
    public UbicacionLoteResponse asignarUbicacion(UbicacionLoteRequest request) {
        String sql = "SELECT fn_asignar_ubicacion(?, ?, ?, ?, ?, ?)";
        Long ubicacionId = jdbcTemplate.queryForObject(sql, Long.class,
                request.getLoteDetalleId(),
                request.getRackId(),
                request.getNivel(),
                request.getColumna(),
                request.getProfundidadIndex(),
                request.getCantidad()
        );
        UbicacionLote ubicacion = ubicacionRepository.findById(ubicacionId)
                .orElseThrow(() -> new EntityNotFoundException("Ubicación no encontrada después de crearla"));
        return mapToResponse(ubicacion);
    }

    public List<UbicacionLoteResponse> listarUbicacionesPorRack(Long rackId) {
        return ubicacionRepository.findByRackIdAndActivoTrueAndRackActivoTrue(rackId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<UbicacionLoteResponse> listarUbicacionesPorLoteDetalle(Long loteDetalleId) {
        return ubicacionRepository.findByLoteDetalleIdAndActivoTrueAndRackActivoTrueOrderById(loteDetalleId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UbicacionLoteResponse obtenerUbicacion(Long id) {
        return mapToResponse(obtenerUbicacionEntity(id));
    }

    /**
     * Elimina lógicamente una ubicación usando la función fn_eliminar_ubicacion.
     */
    @Transactional
    public void eliminarUbicacion(Long id) {
        jdbcTemplate.update("SELECT fn_eliminar_ubicacion(?)", id);
    }

    private UbicacionLote obtenerUbicacionEntity(Long id) {
        return ubicacionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ubicación no encontrada"));
    }

    private UbicacionLoteResponse mapToResponse(UbicacionLote u) {
        UbicacionLoteResponse response = new UbicacionLoteResponse();
        response.setId(u.getId());
        response.setRackId(u.getRack().getId());
        response.setRackNombre(u.getRack().getNombre());
        response.setLoteDetalleId(u.getLoteDetalle().getId());
        response.setMedicamentoNombre(u.getLoteDetalle().getMedicamento().getNombre());
        response.setNivel(u.getNivel());
        response.setColumna(u.getColumna());
        response.setProfundidadIndex(u.getProfundidadIndex());
        response.setCantidad(u.getCantidad());
        response.setActivo(u.getActivo());
        return response;
    }
}