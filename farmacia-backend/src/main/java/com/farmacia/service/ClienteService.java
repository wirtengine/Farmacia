package com.farmacia.service;

import com.farmacia.dto.ClienteRequestDTO;
import com.farmacia.dto.ClienteResponseDTO;
import com.farmacia.model.Cliente;
import com.farmacia.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    // Listar todos los clientes activos
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // Buscar clientes por término (nombre o identificación)
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> buscar(String termino) {
        List<Cliente> porNombre = clienteRepository.findByActivoTrueAndNombreCompletoContainingIgnoreCase(termino);
        List<Cliente> porIdentificacion = clienteRepository.findByActivoTrueAndIdentificacionContaining(termino);
        porNombre.addAll(porIdentificacion);
        return porNombre.stream()
                .distinct()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // Obtener cliente por ID
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        if (!cliente.getActivo()) {
            throw new RuntimeException("Cliente desactivado");
        }
        return convertToResponseDTO(cliente);
    }

    // Crear nuevo cliente
    @Transactional
    public ClienteResponseDTO crearCliente(ClienteRequestDTO request) {
        // Validar que no exista la identificación
        if (clienteRepository.findByIdentificacion(request.getIdentificacion()).isPresent()) {
            throw new RuntimeException("Ya existe un cliente con esa identificación");
        }

        Cliente cliente = new Cliente();
        cliente.setNombreCompleto(request.getNombreCompleto());
        cliente.setIdentificacion(request.getIdentificacion().toUpperCase());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setDireccion(request.getDireccion());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setObservaciones(request.getObservaciones());
        cliente.setActivo(true);

        clienteRepository.save(cliente);
        return convertToResponseDTO(cliente);
    }

    // Actualizar cliente
    @Transactional
    public ClienteResponseDTO actualizarCliente(Long id, ClienteRequestDTO request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Si cambia la identificación, verificar que no exista otra con esa identificación
        if (!cliente.getIdentificacion().equalsIgnoreCase(request.getIdentificacion())) {
            if (clienteRepository.findByIdentificacion(request.getIdentificacion()).isPresent()) {
                throw new RuntimeException("Ya existe otro cliente con esa identificación");
            }
            cliente.setIdentificacion(request.getIdentificacion().toUpperCase());
        }

        cliente.setNombreCompleto(request.getNombreCompleto());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setDireccion(request.getDireccion());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setObservaciones(request.getObservaciones());

        clienteRepository.save(cliente);
        return convertToResponseDTO(cliente);
    }

    // Desactivar cliente (borrado lógico)
    @Transactional
    public void desactivarCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    // Conversión a DTO
    private ClienteResponseDTO convertToResponseDTO(Cliente cliente) {
        ClienteResponseDTO dto = new ClienteResponseDTO();
        dto.setId(cliente.getId());
        dto.setNombreCompleto(cliente.getNombreCompleto());
        dto.setIdentificacion(cliente.getIdentificacion());
        dto.setTelefono(cliente.getTelefono());
        dto.setEmail(cliente.getEmail());
        dto.setDireccion(cliente.getDireccion());
        dto.setFechaNacimiento(cliente.getFechaNacimiento());
        dto.setFechaRegistro(cliente.getFechaRegistro());
        dto.setActivo(cliente.getActivo());
        dto.setObservaciones(cliente.getObservaciones());
        return dto;
    }
}