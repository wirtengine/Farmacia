package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.UbicacionLoteRequest;
import com.farmacia.sanidadbackend.dto.UbicacionLoteResponse;
import com.farmacia.sanidadbackend.model.LoteDetalle;
import com.farmacia.sanidadbackend.model.Rack;
import com.farmacia.sanidadbackend.model.UbicacionLote;
import com.farmacia.sanidadbackend.repository.LoteDetalleRepository;
import com.farmacia.sanidadbackend.repository.RackRepository;
import com.farmacia.sanidadbackend.repository.UbicacionLoteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UbicacionService {

    private final UbicacionLoteRepository ubicacionRepository;
    private final RackRepository rackRepository;
    private final LoteDetalleRepository loteDetalleRepository;

    public List<UbicacionLoteResponse> listarTodasUbicaciones() {
        return ubicacionRepository.findByActivoTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public UbicacionLoteResponse asignarUbicacion(UbicacionLoteRequest request) {
        Rack rack = rackRepository.findById(request.getRackId())
                .orElseThrow(() -> new EntityNotFoundException("Rack no encontrado"));

        LoteDetalle loteDetalle = loteDetalleRepository.findById(request.getLoteDetalleId())
                .orElseThrow(() -> new EntityNotFoundException("LoteDetalle no encontrado"));

        if (request.getNivel() >= rack.getAlto() ||
                request.getColumna() >= rack.getAncho() ||
                request.getProfundidadIndex() >= rack.getProfundidad()) {
            throw new IllegalArgumentException("Coordenadas fuera del rango del rack");
        }

        if (ubicacionRepository.findByCoordenadas(
                rack.getId(),
                request.getNivel(),
                request.getColumna(),
                request.getProfundidadIndex()
        ).isPresent()) {
            throw new IllegalStateException("La celda ya está ocupada");
        }

        int yaAsignado = ubicacionRepository
                .findByLoteDetalleIdAndActivoTrue(request.getLoteDetalleId())
                .stream()
                .mapToInt(UbicacionLote::getCantidad)
                .sum();

        int nuevoTotal = yaAsignado + request.getCantidad();

        if (nuevoTotal > loteDetalle.getCantidad()) {
            throw new IllegalStateException(
                    String.format(
                            "Stock insuficiente. Ya hay %d unidades asignadas. Solo quedan %d disponibles.",
                            yaAsignado,
                            loteDetalle.getCantidad() - yaAsignado
                    )
            );
        }

        UbicacionLote ubicacion = new UbicacionLote();
        ubicacion.setRack(rack);
        ubicacion.setLoteDetalle(loteDetalle);
        ubicacion.setNivel(request.getNivel());
        ubicacion.setColumna(request.getColumna());
        ubicacion.setProfundidadIndex(request.getProfundidadIndex());
        ubicacion.setCantidad(request.getCantidad());
        ubicacion.setActivo(true);

        return mapToResponse(ubicacionRepository.save(ubicacion));
    }

    public List<UbicacionLoteResponse> listarUbicacionesPorRack(Long rackId) {
        return ubicacionRepository.findByRackIdAndActivoTrue(rackId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UbicacionLoteResponse obtenerUbicacion(Long id) {
        return mapToResponse(obtenerUbicacionEntity(id));
    }

    @Transactional
    public void eliminarUbicacion(Long id) {
        UbicacionLote ubicacion = obtenerUbicacionEntity(id);
        ubicacion.setActivo(false);
        ubicacionRepository.save(ubicacion);
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