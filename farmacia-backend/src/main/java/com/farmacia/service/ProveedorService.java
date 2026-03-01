package com.farmacia.service;

import com.farmacia.dto.ProveedorRequestDTO;
import com.farmacia.dto.ProveedorResponseDTO;
import com.farmacia.model.Proveedor;
import com.farmacia.repository.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProveedorService {

    @Autowired
    private ProveedorRepository proveedorRepository;

    // Listar todos los proveedores activos (para admin y vendedor)
    @Transactional(readOnly = true)
    public List<ProveedorResponseDTO> listarActivos() {
        return proveedorRepository.findByActivoTrue().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // Buscar proveedores por nombre o RUC (filtro)
    @Transactional(readOnly = true)
    public List<ProveedorResponseDTO> buscar(String termino) {
        return proveedorRepository.findByNombreContainingIgnoreCaseOrRucContainingIgnoreCaseAndActivoTrue(termino, termino)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // Obtener proveedor por ID
    @Transactional(readOnly = true)
    public ProveedorResponseDTO obtenerPorId(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        if (!proveedor.getActivo()) {
            throw new RuntimeException("Proveedor desactivado");
        }
        return convertToResponseDTO(proveedor);
    }

    // Crear nuevo proveedor
    @Transactional
    public ProveedorResponseDTO crearProveedor(ProveedorRequestDTO request) {
        // Verificar si ya existe un proveedor con ese RUC
        if (proveedorRepository.findByRuc(request.getRuc()).isPresent()) {
            throw new RuntimeException("Ya existe un proveedor con ese RUC");
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(request.getNombre());
        proveedor.setRuc(request.getRuc());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setEmail(request.getEmail());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefonoContacto(request.getTelefonoContacto());
        proveedor.setActivo(true);

        proveedorRepository.save(proveedor);
        return convertToResponseDTO(proveedor);
    }

    // Actualizar proveedor
    @Transactional
    public ProveedorResponseDTO actualizarProveedor(Long id, ProveedorRequestDTO request) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        // Si cambia el RUC, verificar que no esté duplicado
        if (!proveedor.getRuc().equals(request.getRuc())) {
            if (proveedorRepository.findByRuc(request.getRuc()).isPresent()) {
                throw new RuntimeException("Ya existe un proveedor con ese RUC");
            }
        }

        proveedor.setNombre(request.getNombre());
        proveedor.setRuc(request.getRuc());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setEmail(request.getEmail());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefonoContacto(request.getTelefonoContacto());

        proveedorRepository.save(proveedor);
        return convertToResponseDTO(proveedor);
    }

    // Desactivar proveedor (borrado lógico)
    @Transactional
    public void desactivarProveedor(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
    }

    // Método auxiliar para convertir entidad a DTO
    private ProveedorResponseDTO convertToResponseDTO(Proveedor proveedor) {
        ProveedorResponseDTO dto = new ProveedorResponseDTO();
        dto.setId(proveedor.getId());
        dto.setNombre(proveedor.getNombre());
        dto.setRuc(proveedor.getRuc());
        dto.setTelefono(proveedor.getTelefono());
        dto.setEmail(proveedor.getEmail());
        dto.setDireccion(proveedor.getDireccion());
        dto.setContacto(proveedor.getContacto());
        dto.setTelefonoContacto(proveedor.getTelefonoContacto());
        dto.setActivo(proveedor.getActivo());
        return dto;
    }
}