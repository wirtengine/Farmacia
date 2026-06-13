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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class DevolucionProveedorService {

    private final JdbcTemplate jdbcTemplate;
    private final DevolucionProveedorRepository devolucionProveedorRepository;
    private final LoteRepository loteRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteDetalleRepository loteDetalleRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String generarNumeroDevolucion() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String numero;
        do {
            int random = ThreadLocalRandom.current().nextInt(1000, 10000);
            numero = "DPROV-" + fecha + "-" + random;
        } while (devolucionProveedorRepository.existsByNumeroDevolucion(numero));
        return numero;
    }

    /**
     * Solicita una devolución a proveedor usando la función fn_solicitar_devolucion_proveedor.
     * Parámetros: lote_id, solicitado_por_id, detalles_jsonb, motivo
     */
    public DevolucionProveedorResponse solicitarDevolucion(DevolucionProveedorRequest request) {
        String detallesJson;
        try {
            detallesJson = objectMapper.writeValueAsString(request.getDetalles());
        } catch (Exception e) {
            throw new RuntimeException("Error al convertir detalles a JSON", e);
        }

        String sql = "SELECT * FROM fn_solicitar_devolucion_proveedor(?, ?, ?::jsonb, ?)";
        Map<String, Object> result = jdbcTemplate.queryForMap(sql,
                request.getLoteId(),
                request.getSolicitadoPorId(),
                detallesJson,
                request.getMotivo()
        );

        Long devolucionId = ((Number) result.get("devolucion_id")).longValue();
        DevolucionProveedor dev = devolucionProveedorRepository.findById(devolucionId)
                .orElseThrow(() -> new EntityNotFoundException("Devolución no encontrada después de crearla"));
        return mapToResponse(dev);
    }

    /**
     * Aprueba o rechaza una devolución a proveedor (lógica manual en Java).
     */
    public DevolucionProveedorResponse aprobarDevolucion(DevolucionProveedorAprobarRequest request) {
        DevolucionProveedor devolucion = devolucionProveedorRepository.findById(request.getDevolucionId())
                .orElseThrow(() -> new EntityNotFoundException("Devolución no encontrada"));

        if (devolucion.getEstado() != EstadoDevolucionProveedor.PENDIENTE) {
            throw new IllegalStateException("La devolución ya fue procesada");
        }

        Usuario admin = usuarioRepository.findById(request.getAprobadoPorId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        // Rechazo
        if (Boolean.FALSE.equals(request.getAprobada())) {
            devolucion.setEstado(EstadoDevolucionProveedor.RECHAZADA);
            devolucion.setMotivo(request.getMotivoRechazo());
            devolucion.setAprobadoPor(admin);
            devolucion.setFechaAprobacion(LocalDateTime.now());
            return mapToResponse(devolucionProveedorRepository.save(devolucion));
        }

        // Aprobación
        devolucion.setEstado(EstadoDevolucionProveedor.APROBADA);
        devolucion.setNumeroDevolucion(generarNumeroDevolucion());
        devolucion.setAprobadoPor(admin);
        devolucion.setFechaAprobacion(LocalDateTime.now());

        for (DevolucionProveedorDetalle det : devolucion.getDetalles()) {
            LoteDetalle loteDetalle = det.getLoteDetalle();
            loteDetalle.setCantidad(loteDetalle.getCantidad() - det.getCantidadDevuelta());
            loteDetalleRepository.save(loteDetalle);
        }

        DevolucionProveedor saved = devolucionProveedorRepository.save(devolucion);
        return mapToResponse(saved);
    }

    public List<DevolucionProveedorResponse> listarDevoluciones() {
        return devolucionProveedorRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public DevolucionProveedorResponse obtenerDevolucion(Long id) {
        DevolucionProveedor dev = devolucionProveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Devolución no encontrada"));
        return mapToResponse(dev);
    }

    private DevolucionProveedorResponse mapToResponse(DevolucionProveedor d) {
        DevolucionProveedorResponse resp = new DevolucionProveedorResponse();
        resp.setId(d.getId());
        resp.setNumeroDevolucion(d.getNumeroDevolucion());
        resp.setLoteId(d.getLote().getId());
        resp.setNumeroFacturaLote(d.getLote().getFactura());
        resp.setProveedorId(d.getProveedor().getId());
        resp.setProveedorNombre(d.getProveedor().getNombre());
        resp.setProveedorTelefono(d.getProveedor().getTelefono());
        resp.setProveedorEmail(d.getProveedor().getEmail());

        resp.setSolicitadoPorId(d.getSolicitadoPor().getId());
        resp.setSolicitadoPorNombre(d.getSolicitadoPor().getUsername());

        if (d.getAprobadoPor() != null) {
            resp.setAprobadoPorId(d.getAprobadoPor().getId());
            resp.setAprobadoPorNombre(d.getAprobadoPor().getUsername());
        }

        resp.setEstado(d.getEstado().name());
        resp.setMotivo(d.getMotivo());
        resp.setFechaSolicitud(d.getFechaSolicitud());
        resp.setFechaAprobacion(d.getFechaAprobacion());

        List<DevolucionProveedorDetalleResponse> detalles = d.getDetalles().stream()
                .map(det -> {
                    DevolucionProveedorDetalleResponse dr = new DevolucionProveedorDetalleResponse();
                    dr.setId(det.getId());
                    dr.setLoteDetalleId(det.getLoteDetalle().getId());
                    dr.setMedicamentoNombre(det.getLoteDetalle().getMedicamento().getNombre());
                    dr.setLoteNumero(det.getLoteDetalle().getLote().getNumeroLote());
                    dr.setCantidadDevuelta(det.getCantidadDevuelta());
                    return dr;
                })
                .toList();

        resp.setDetalles(detalles);
        return resp;
    }
}