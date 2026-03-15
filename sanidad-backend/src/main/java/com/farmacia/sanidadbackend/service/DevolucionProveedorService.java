package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.*;
import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DevolucionProveedorService {

    private final DevolucionProveedorRepository devolucionProveedorRepository;
    private final LoteRepository loteRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteDetalleRepository loteDetalleRepository;

    private String generarNumeroDevolucion() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String numero;
        do {
            int random = ThreadLocalRandom.current().nextInt(1000, 10000);
            numero = "DPROV-" + fecha + "-" + random;
        } while (devolucionProveedorRepository.existsByNumeroDevolucion(numero));
        return numero;
    }

    public DevolucionProveedorResponse solicitarDevolucion(DevolucionProveedorRequest request) {

        Lote lote = loteRepository.findByIdAndActivoTrue(request.getLoteId())
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado o inactivo"));

        Proveedor proveedor = lote.getProveedor(); // El lote ya tiene proveedor

        Usuario solicitante = usuarioRepository.findById(request.getSolicitadoPorId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        DevolucionProveedor devolucion = new DevolucionProveedor();
        devolucion.setLote(lote);
        devolucion.setProveedor(proveedor);
        devolucion.setSolicitadoPor(solicitante);
        devolucion.setEstado(EstadoDevolucionProveedor.PENDIENTE);
        devolucion.setMotivo(request.getMotivo());

        List<DevolucionProveedorDetalle> detalles = request.getDetalles().stream()
                .map(detReq -> {
                    LoteDetalle loteDetalle = loteDetalleRepository.findById(detReq.getLoteDetalleId())
                            .orElseThrow(() -> new EntityNotFoundException("Detalle de lote no encontrado"));

                    if (!loteDetalle.getLote().getId().equals(lote.getId())) {
                        throw new IllegalArgumentException("El detalle no pertenece al lote seleccionado");
                    }

                    if (detReq.getCantidadDevuelta() > loteDetalle.getCantidad()) {
                        throw new IllegalArgumentException("Cantidad a devolver excede el stock disponible");
                    }

                    DevolucionProveedorDetalle detalle = new DevolucionProveedorDetalle();
                    detalle.setDevolucionProveedor(devolucion);
                    detalle.setLoteDetalle(loteDetalle);
                    detalle.setCantidadDevuelta(detReq.getCantidadDevuelta());
                    return detalle;
                })
                .collect(Collectors.toList());

        devolucion.setDetalles(detalles);

        DevolucionProveedor saved = devolucionProveedorRepository.save(devolucion);
        return mapToResponse(saved);
    }

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
            devolucion.setMotivo(request.getMotivoRechazo()); // sobrescribe el motivo original o usa otro campo
            devolucion.setAprobadoPor(admin);
            devolucion.setFechaAprobacion(LocalDateTime.now());
            return mapToResponse(devolucionProveedorRepository.save(devolucion));
        }

        // Aprobación
        devolucion.setEstado(EstadoDevolucionProveedor.APROBADA);
        devolucion.setNumeroDevolucion(generarNumeroDevolucion());
        devolucion.setAprobadoPor(admin);
        devolucion.setFechaAprobacion(LocalDateTime.now());

        // Descontar stock del lote
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