package com.farmacia.service;

import com.farmacia.dto.*;
import com.farmacia.enums.EstadoDevolucion;
import com.farmacia.enums.EstadoVenta;
import com.farmacia.model.*;
import com.farmacia.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DevolucionService {

    @Autowired
    private DevolucionRepository devolucionRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private VentaDetalleRepository ventaDetalleRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LoteService loteService;

    @Transactional
    public DevolucionResponseDTO solicitarDevolucion(DevolucionRequestDTO request) {
        Usuario vendedor = getCurrentUser();

        Venta venta = ventaRepository.findById(request.getVentaId())
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        VentaDetalle detalle = null;
        if (request.getDetalleId() != null) {
            detalle = ventaDetalleRepository.findById(request.getDetalleId())
                    .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));
            if (!detalle.getVenta().getId().equals(venta.getId())) {
                throw new RuntimeException("El detalle no pertenece a la venta indicada");
            }
        }

        // Validar cantidad
        if (detalle != null && request.getCantidad() > detalle.getCantidad()) {
            throw new RuntimeException("La cantidad a devolver excede la cantidad vendida");
        }

        Devolucion devolucion = new Devolucion();
        devolucion.setVenta(venta);
        devolucion.setDetalle(detalle);
        devolucion.setVendedor(vendedor);
        devolucion.setCantidad(request.getCantidad());
        devolucion.setMotivo(request.getMotivo());
        devolucion.setEstado(EstadoDevolucion.PENDIENTE);

        devolucion = devolucionRepository.save(devolucion);
        return mapToResponseDTO(devolucion);
    }

    @Transactional
    public DevolucionResponseDTO procesarDevolucion(DevolucionAprobacionDTO request) {
        Usuario admin = getCurrentUser();

        Devolucion devolucion = devolucionRepository.findById(request.getDevolucionId())
                .orElseThrow(() -> new RuntimeException("Devolución no encontrada"));

        if (devolucion.getEstado() != EstadoDevolucion.PENDIENTE) {
            throw new RuntimeException("La devolución ya fue procesada");
        }

        if ("APROBAR".equalsIgnoreCase(request.getAccion())) {
            Venta venta = devolucion.getVenta();
            VentaDetalle detalle = devolucion.getDetalle();

            if (detalle != null) {
                // Devolución parcial de un ítem específico
                loteService.agregarStock(
                        detalle.getLote().getId(),
                        devolucion.getCantidad(),
                        "Devolución parcial aprobada - Venta " + venta.getNumeroFactura()
                );
                System.out.println("Devolución parcial - venta " + venta.getNumeroFactura() + " no se anula.");
            } else {
                // Devolución total de la venta
                venta.getDetalles().forEach(d -> {
                    loteService.agregarStock(
                            d.getLote().getId(),
                            d.getCantidad(),
                            "Devolución total aprobada - Venta " + venta.getNumeroFactura()
                    );
                });
                // ANULAR LA VENTA
                venta.setEstado(EstadoVenta.ANULADA);
                ventaRepository.save(venta);
                System.out.println("Devolución total - venta " + venta.getNumeroFactura() + " ANULADA.");
            }

            devolucion.setEstado(EstadoDevolucion.APROBADA);
        } else if ("RECHAZAR".equalsIgnoreCase(request.getAccion())) {
            devolucion.setEstado(EstadoDevolucion.RECHAZADA);
        } else {
            throw new RuntimeException("Acción no válida");
        }

        devolucion.setAdmin(admin);
        devolucion.setObservacionAdmin(request.getObservacion());
        devolucion.setFechaAprobacion(LocalDateTime.now());

        devolucion = devolucionRepository.save(devolucion);
        return mapToResponseDTO(devolucion);
    }

    public DevolucionResponseDTO obtenerPorId(Long id) {
        Devolucion dev = devolucionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devolución no encontrada"));
        return mapToResponseDTO(dev);
    }

    public List<DevolucionResponseDTO> listarTodas() {
        return devolucionRepository.findAllByOrderByFechaSolicitudDesc().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<DevolucionResponseDTO> listarPendientes() {
        return devolucionRepository.findByEstadoOrderByFechaSolicitudAsc(EstadoDevolucion.PENDIENTE)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<DevolucionResponseDTO> listarPorVendedor(Long vendedorId) {
        Usuario vendedor = usuarioRepository.findById(vendedorId)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));
        return devolucionRepository.findByVendedorOrderByFechaSolicitudDesc(vendedor).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private Usuario getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        }
        throw new RuntimeException("Usuario no autenticado");
    }

    private DevolucionResponseDTO mapToResponseDTO(Devolucion dev) {
        DevolucionResponseDTO dto = new DevolucionResponseDTO();
        dto.setId(dev.getId());
        dto.setVentaId(dev.getVenta().getId());
        dto.setNumeroFactura(dev.getVenta().getNumeroFactura());
        if (dev.getDetalle() != null) {
            dto.setDetalleId(dev.getDetalle().getId());
            dto.setMedicamentoNombre(dev.getDetalle().getMedicamento().getNombre());
        }
        dto.setCantidad(dev.getCantidad());
        dto.setMotivo(dev.getMotivo());
        dto.setEstado(dev.getEstado().toString());
        dto.setFechaSolicitud(dev.getFechaSolicitud());
        dto.setVendedorNombre(dev.getVendedor().getNombre() + " " + dev.getVendedor().getApellido());
        if (dev.getAdmin() != null) {
            dto.setAdminNombre(dev.getAdmin().getNombre() + " " + dev.getAdmin().getApellido());
        }
        dto.setFechaAprobacion(dev.getFechaAprobacion());
        dto.setObservacionAdmin(dev.getObservacionAdmin());
        return dto;
    }
}