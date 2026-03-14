package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.ProveedorRequest;
import com.farmacia.sanidadbackend.dto.ProveedorResponse;
import com.farmacia.sanidadbackend.model.Proveedor;
import com.farmacia.sanidadbackend.repository.ProveedorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional
    public ProveedorResponse crearProveedor(ProveedorRequest request) {

        if (proveedorRepository.existsByRuc(request.getRuc())) {
            throw new IllegalArgumentException("Ya existe un proveedor con ese RUC");
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setRuc(request.getRuc());
        proveedor.setNombre(request.getNombre());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setEmail(request.getEmail());
        proveedor.setActivo(true);

        return mapToResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponse actualizarProveedor(Long id, ProveedorRequest request) {

        Proveedor proveedor = obtenerProveedorActivo(id);

        if (!proveedor.getRuc().equals(request.getRuc())
                && proveedorRepository.existsByRuc(request.getRuc())) {

            throw new IllegalArgumentException("Ya existe un proveedor con ese RUC");
        }

        proveedor.setRuc(request.getRuc());
        proveedor.setNombre(request.getNombre());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setEmail(request.getEmail());

        return mapToResponse(proveedorRepository.save(proveedor));
    }

    public List<ProveedorResponse> listarProveedores() {

        return proveedorRepository.findByActivoTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProveedorResponse obtenerProveedor(Long id) {

        return mapToResponse(obtenerProveedorActivo(id));
    }

    @Transactional
    public void suspenderProveedor(Long id) {

        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado"));

        proveedor.setActivo(false);

        proveedorRepository.save(proveedor);
    }

    private Proveedor obtenerProveedorActivo(Long id) {

        return proveedorRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado"));
    }

    private ProveedorResponse mapToResponse(Proveedor proveedor) {

        ProveedorResponse response = new ProveedorResponse();

        response.setId(proveedor.getId());
        response.setRuc(proveedor.getRuc());
        response.setNombre(proveedor.getNombre());
        response.setTelefono(proveedor.getTelefono());
        response.setEmail(proveedor.getEmail());

        // Si activo es boolean
        response.setActivo(proveedor.getActivo());

        return response;
    }
}