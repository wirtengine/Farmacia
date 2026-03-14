package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.ClienteRequest;
import com.farmacia.sanidadbackend.dto.ClienteResponse;
import com.farmacia.sanidadbackend.model.Cliente;
import com.farmacia.sanidadbackend.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request) {
        if (clienteRepository.existsByCedula(request.getCedula())) {
            throw new IllegalArgumentException("Ya existe un cliente con esa cédula");
        }

        Cliente cliente = new Cliente();
        cliente.setCedula(request.getCedula());
        cliente.setNombre(request.getNombre());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setSaldo(request.getSaldo() != null ? request.getSaldo() : BigDecimal.ZERO);
        cliente.setActivo(true);

        return mapToResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizarCliente(Long id, ClienteRequest request) {
        Cliente cliente = obtenerClienteActivo(id);

        if (!cliente.getCedula().equals(request.getCedula()) &&
                clienteRepository.existsByCedula(request.getCedula())) {
            throw new IllegalArgumentException("Ya existe un cliente con esa cédula");
        }

        cliente.setCedula(request.getCedula());
        cliente.setNombre(request.getNombre());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setSaldo(request.getSaldo()); // <-- CAMBIO IMPORTANTE

        return mapToResponse(clienteRepository.save(cliente));
    }

    public List<ClienteResponse> listarClientes() {
        return clienteRepository.findAllByActivoTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ClienteResponse obtenerCliente(Long id) {
        return mapToResponse(obtenerClienteActivo(id));
    }

    @Transactional
    public void suspenderCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    @Transactional
    public ClienteResponse abonarSaldo(Long id, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        Cliente cliente = obtenerClienteActivo(id);
        cliente.setSaldo(cliente.getSaldo().add(monto));

        return mapToResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void descontarSaldo(Long id, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        Cliente cliente = obtenerClienteActivo(id);
        BigDecimal nuevoSaldo = cliente.getSaldo().subtract(monto);

        if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Saldo insuficiente");
        }

        cliente.setSaldo(nuevoSaldo);
        clienteRepository.save(cliente);
    }

    private Cliente obtenerClienteActivo(Long id) {
        return clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado o suspendido"));
    }

    private ClienteResponse mapToResponse(Cliente cliente) {
        ClienteResponse response = new ClienteResponse();
        response.setId(cliente.getId());
        response.setCedula(cliente.getCedula());
        response.setNombre(cliente.getNombre());
        response.setTelefono(cliente.getTelefono());
        response.setEmail(cliente.getEmail());
        response.setSaldo(cliente.getSaldo());
        response.setActivo(cliente.getActivo());
        return response;
    }
}