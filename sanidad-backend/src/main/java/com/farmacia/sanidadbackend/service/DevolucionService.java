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

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DevolucionService {

    private final JdbcTemplate jdbcTemplate;
    private final DevolucionRepository devolucionRepository;
    // Estos repositorios ya no se usan en los nuevos métodos, pero se dejan por si otros métodos los necesitan
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteDetalleRepository loteDetalleRepository;
    private final ClienteService clienteService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Solicita una devolución usando la función fn_solicitar_devolucion.
     */
    public DevolucionResponse solicitarDevolucion(DevolucionRequest request) {
        String detallesJson;
        try {
            detallesJson = objectMapper.writeValueAsString(request.getDetalles());
        } catch (Exception e) {
            throw new RuntimeException("Error al convertir detalles a JSON", e);
        }

        String sql = "SELECT * FROM fn_solicitar_devolucion(?, ?, ?, ?::jsonb)";
        Map<String, Object> result = jdbcTemplate.queryForMap(sql,
                request.getVentaId(),
                request.getSolicitadoPorId(),
                request.getMotivo(),
                detallesJson
        );

        Long devolucionId = ((Number) result.get("devolucion_id")).longValue();
        Devolucion dev = devolucionRepository.findById(devolucionId)
                .orElseThrow(() -> new EntityNotFoundException("Devolución no encontrada después de crearla"));
        return mapToResponse(dev);
    }

    /**
     * Aprueba o rechaza una devolución usando la función fn_aprobar_devolucion.
     */
    public DevolucionResponse aprobarDevolucion(DevolucionAprobarRequest request) {
        String sql = "SELECT * FROM fn_aprobar_devolucion(?, ?, ?, ?)";
        Map<String, Object> result = jdbcTemplate.queryForMap(sql,
                request.getDevolucionId(),
                request.getAprobadoPorId(),
                request.getAprobada(),
                request.getMotivoRechazo()
        );

        Long devolucionId = ((Number) result.get("devolucion_id")).longValue();
        Devolucion dev = devolucionRepository.findById(devolucionId)
                .orElseThrow(() -> new EntityNotFoundException("Devolución no encontrada después del proceso"));
        return mapToResponse(dev);
    }

    public List<DevolucionResponse> listarDevoluciones() {
        return devolucionRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public DevolucionResponse obtenerDevolucion(Long id) {
        Devolucion devolucion = devolucionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Devolución no encontrada"));
        return mapToResponse(devolucion);
    }

    private DevolucionResponse mapToResponse(Devolucion d) {
        DevolucionResponse resp = new DevolucionResponse();
        resp.setId(d.getId());
        resp.setNumeroDevolucion(d.getNumeroDevolucion());
        resp.setVentaId(d.getVenta().getId());
        resp.setNumeroFactura(d.getVenta().getNumeroFactura());

        resp.setUsuarioSolicitanteId(d.getSolicitadoPor().getId());
        resp.setUsuarioSolicitanteNombre(d.getSolicitadoPor().getUsername());

        if (d.getAprobadoPor() != null) {
            resp.setUsuarioApruebaId(d.getAprobadoPor().getId());
            resp.setUsuarioApruebaNombre(d.getAprobadoPor().getUsername());
        }

        resp.setEstado(d.getEstado().name());
        resp.setMotivo(d.getMotivo());
        resp.setFechaSolicitud(d.getFechaSolicitud());
        resp.setFechaAprobacion(d.getFechaAprobacion());

        resp.setSubtotalDevuelto(d.getSubtotalDevuelto());
        resp.setIvaDevuelto(d.getIvaDevuelto());
        resp.setTotalDevuelto(d.getTotalDevuelto());
        resp.setMontoDevueltoEfectivo(d.getMontoDevueltoEfectivo());
        resp.setMontoDevueltoSaldo(d.getMontoDevueltoSaldo());

        List<DevolucionDetalleResponse> detalles = d.getDetalles().stream()
                .map(det -> {
                    DevolucionDetalleResponse dr = new DevolucionDetalleResponse();
                    dr.setId(det.getId());
                    dr.setLoteDetalleId(det.getLoteDetalle().getId());
                    dr.setMedicamentoNombre(det.getLoteDetalle().getMedicamento().getNombre());
                    dr.setLoteNumero(det.getLoteDetalle().getLote().getNumeroLote());
                    dr.setCantidadDevuelta(det.getCantidadDevuelta());
                    dr.setPrecioUnitario(det.getPrecioUnitario());
                    dr.setSubtotal(det.getSubtotal());
                    return dr;
                })
                .toList();
        resp.setDetalles(detalles);
        return resp;
    }
}