package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.ClienteRequest;
import com.farmacia.sanidadbackend.dto.ClienteResponse;
import com.farmacia.sanidadbackend.model.Cliente;
import com.farmacia.sanidadbackend.repository.ClienteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final JdbcTemplate jdbcTemplate;
    private final ClienteRepository clienteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        cliente.setSaldo(request.getSaldo());

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

    /**
     * Abona saldo a un cliente usando la función fn_abonar_saldo.
     */
    @Transactional
    public ClienteResponse abonarSaldo(Long clienteId, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        String sql = "SELECT fn_abonar_saldo(?, ?)";
        jdbcTemplate.queryForObject(sql, BigDecimal.class, clienteId, monto);

        // Recuperar cliente actualizado para devolver respuesta completa
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado después del abono"));
        return mapToResponse(cliente);
    }

    /**
     * Descuenta saldo a un cliente usando la función fn_descontar_saldo.
     */
    @Transactional
    public ClienteResponse descontarSaldo(Long clienteId, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }

        String sql = "SELECT fn_descontar_saldo(?, ?)";
        jdbcTemplate.queryForObject(sql, BigDecimal.class, clienteId, monto);

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado después del descuento"));
        return mapToResponse(cliente);
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