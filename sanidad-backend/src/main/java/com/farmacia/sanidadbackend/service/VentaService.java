package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.*;
import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final JdbcTemplate jdbcTemplate;
    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteDetalleRepository loteDetalleRepository;
    private final ClienteService clienteService;
    private final UbicacionLoteRepository ubicacionLoteRepository;
    private final RecetaService recetaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Crea una venta usando la función fn_crear_venta.
     */
    @Transactional
    public VentaResponse crearVenta(VentaRequest request) {
        // Validaciones previas rápidas (opcional, también las hace la función)
        usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (request.getClienteId() != null) {
            clienteRepository.findByIdAndActivoTrue(request.getClienteId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado o inactivo"));
        }

        String detallesJson;
        try {
            detallesJson = objectMapper.writeValueAsString(request.getDetalles());
        } catch (Exception e) {
            throw new RuntimeException("Error al convertir detalles a JSON", e);
        }

        // Llamada a la función con SELECT * FROM ...
        String sql = "SELECT * FROM fn_crear_venta(?, ?, ?::jsonb, ?, ?, ?)";
        Map<String, Object> result = jdbcTemplate.queryForMap(sql,
                request.getClienteId(),
                request.getUsuarioId(),
                detallesJson,
                request.getRecetaId(),
                request.getMontoEfectivo() != null ? request.getMontoEfectivo() : BigDecimal.ZERO,
                request.getMontoUsadoSaldo() != null ? request.getMontoUsadoSaldo() : BigDecimal.ZERO
        );

        Long ventaId = ((Number) result.get("venta_id")).longValue();
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada después de crearla"));
        return mapToResponse(venta);
    }

    // Métodos sin cambios
    public List<VentaResponse> listarVentas() {
        return ventaRepository.findByActivoTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public VentaResponse obtenerVenta(Long id) {
        Venta venta = ventaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada"));
        return mapToResponse(venta);
    }

    @Transactional
    public void anularVenta(Long id) {
        Venta venta = ventaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada"));
        for (VentaDetalle detalle : venta.getDetalles()) {
            LoteDetalle loteDetalle = detalle.getLoteDetalle();
            loteDetalle.setCantidad(loteDetalle.getCantidad() + detalle.getCantidad());
            loteDetalleRepository.save(loteDetalle);
        }
        venta.setActivo(false);
        ventaRepository.save(venta);
    }

    private VentaResponse mapToResponse(Venta venta) {
        VentaResponse response = new VentaResponse();
        response.setId(venta.getId());
        response.setNumeroFactura(venta.getNumeroFactura());
        response.setFecha(venta.getFecha());

        if (venta.getCliente() != null) {
            response.setClienteId(venta.getCliente().getId());
            response.setClienteNombre(venta.getCliente().getNombre());
            response.setClienteCedula(venta.getCliente().getCedula());
        }

        response.setUsuarioId(venta.getUsuario().getId());
        response.setUsuarioUsername(venta.getUsuario().getUsername());
        response.setSubtotal(venta.getSubtotal());
        response.setIva(venta.getIva());
        response.setTotal(venta.getTotal());
        response.setTipo(venta.getTipo().name());
        response.setMontoUsadoSaldo(venta.getMontoUsadoSaldo());
        response.setMontoEfectivo(venta.getMontoEfectivo());
        response.setCambio(venta.getCambio());

        List<VentaDetalleResponse> detalles = venta.getDetalles()
                .stream()
                .map(detalle -> {
                    VentaDetalleResponse dto = new VentaDetalleResponse();
                    dto.setId(detalle.getId());
                    dto.setLoteDetalleId(detalle.getLoteDetalle().getId());
                    dto.setMedicamentoNombre(detalle.getLoteDetalle().getMedicamento().getNombre());
                    dto.setPresentacion(detalle.getLoteDetalle().getMedicamento().getPresentacion());
                    dto.setLoteNumero(detalle.getLoteDetalle().getLote().getNumeroLote());
                    dto.setCantidad(detalle.getCantidad());
                    dto.setPrecioUnitario(detalle.getPrecioUnitario());
                    dto.setSubtotal(detalle.getSubtotal());
                    return dto;
                })
                .toList();
        response.setDetalles(detalles);
        return response;
    }
}