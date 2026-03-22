package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.RackRequest;
import com.farmacia.sanidadbackend.dto.RackResponse;
import com.farmacia.sanidadbackend.model.Rack;
import com.farmacia.sanidadbackend.repository.RackRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RackService {

    private final RackRepository rackRepository;

    @Transactional
    public RackResponse crearRack(RackRequest request) {
        Rack rack = new Rack();
        rack.setNombre(request.getNombre());
        rack.setDescripcion(request.getDescripcion());
        rack.setAncho(request.getAncho());
        rack.setAlto(request.getAlto());
        rack.setProfundidad(request.getProfundidad());
        rack.setActivo(true);
        return mapToResponse(rackRepository.save(rack));
    }

    @Transactional
    public RackResponse actualizarRack(Long id, RackRequest request) {
        Rack rack = rackRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Rack no encontrado"));
        rack.setNombre(request.getNombre());
        rack.setDescripcion(request.getDescripcion());
        rack.setAncho(request.getAncho());
        rack.setAlto(request.getAlto());
        rack.setProfundidad(request.getProfundidad());
        return mapToResponse(rackRepository.save(rack));
    }

    public List<RackResponse> listarRacks() {
        return rackRepository.findByActivoTrue().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public RackResponse obtenerRack(Long id) {
        Rack rack = rackRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Rack no encontrado"));
        return mapToResponse(rack);
    }

    @Transactional
    public void eliminarRack(Long id) {
        Rack rack = rackRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Rack no encontrado"));
        rack.setActivo(false);
        rackRepository.save(rack);
    }

    private RackResponse mapToResponse(Rack rack) {
        RackResponse response = new RackResponse();
        response.setId(rack.getId());
        response.setNombre(rack.getNombre());
        response.setDescripcion(rack.getDescripcion());
        response.setAncho(rack.getAncho());
        response.setAlto(rack.getAlto());
        response.setProfundidad(rack.getProfundidad());
        response.setActivo(rack.getActivo());
        return response;
    }
}