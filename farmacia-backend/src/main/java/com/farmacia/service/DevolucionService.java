package com.farmacia.service;

import com.farmacia.dto.*;
import com.farmacia.enums.EstadoDevolucion;
import com.farmacia.enums.EstadoVenta;
import com.farmacia.model.*;
import com.farmacia.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DevolucionService {

    private static final Logger log = LoggerFactory.getLogger(DevolucionService.class);

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
        log.info("Solicitud de devolución creada: ID={}, ventaId={}, detalleId={}, cantidad={}",
                devolucion.getId(), venta.getId(), request.getDetalleId(), request.getCantidad());
        return mapToResponseDTO(devolucion);
    }

    @Transactional
    public DevolucionResponseDTO procesarDevolucion(DevolucionAprobacionDTO request) {
        Usuario admin = getCurrentUser();

        Devolucion devolucion = devolucionRepository.findByIdWithVentaAndDetalles(request.getDevolucionId())
                .orElseThrow(() -> new RuntimeException("Devolución no encontrada"));

        if (devolucion.getEstado() != EstadoDevolucion.PENDIENTE) {
            throw new RuntimeException("La devolución ya fue procesada");
        }

        log.info("Procesando devolución ID: {}, estado actual: {}, detalle null? {}",
                devolucion.getId(), devolucion.getEstado(), devolucion.getDetalle() == null);

        try {
            if ("APROBAR".equalsIgnoreCase(request.getAccion())) {
                Venta venta = devolucion.getVenta();
                log.info("Venta ID: {}, detalles size: {}", venta.getId(), venta.getDetalles().size());

                if (devolucion.getDetalle() != null) {
                    // Devolución parcial
                    VentaDetalle detalle = devolucion.getDetalle();
                    int cantidadDevuelta = devolucion.getCantidad();

                    log.info("Devolución PARCIAL: detalleId={}, loteId={}, cantidadADevolver={}",
                            detalle.getId(), detalle.getLote().getId(), cantidadDevuelta);

                    // 1. Devolver stock al lote
                    loteService.agregarStock(
                            detalle.getLote().getId(),
                            cantidadDevuelta,
                            "Devolución parcial aprobada - Venta " + venta.getNumeroFactura()
                    );

                    // 2. Actualizar el detalle de la venta
                    int nuevaCantidad = detalle.getCantidad() - cantidadDevuelta;

                    if (nuevaCantidad == 0) {
                        // Eliminar el detalle completamente
                        venta.getDetalles().remove(detalle);
                        ventaDetalleRepository.delete(detalle);
                        log.info("Detalle eliminado porque la cantidad llegó a cero");
                    } else {
                        detalle.setCantidad(nuevaCantidad);
                        // Recalcular subtotal y ganancia del detalle
                        BigDecimal precioUnitario = detalle.getPrecioUnitario();
                        BigDecimal descuentoItem = detalle.getDescuento() != null ? detalle.getDescuento() : BigDecimal.ZERO;
                        BigDecimal nuevoSubtotal = precioUnitario.multiply(new BigDecimal(nuevaCantidad)).subtract(descuentoItem);
                        detalle.setSubtotal(nuevoSubtotal);

                        BigDecimal costoUnitario = detalle.getCostoUnitario() != null ? detalle.getCostoUnitario() : BigDecimal.ZERO;
                        BigDecimal nuevaGanancia = precioUnitario.subtract(costoUnitario)
                                .multiply(new BigDecimal(nuevaCantidad))
                                .subtract(descuentoItem);
                        detalle.setGanancia(nuevaGanancia);

                        ventaDetalleRepository.save(detalle);
                        log.info("Detalle actualizado: nueva cantidad={}, nuevo subtotal={}", nuevaCantidad, nuevoSubtotal);
                    }

                    // 3. Recalcular totales de la venta
                    recalcularTotalesVenta(venta);

                    // 4. Si la venta se quedó sin detalles, anularla
                    if (venta.getDetalles().isEmpty()) {
                        venta.setEstado(EstadoVenta.ANULADA);
                        ventaRepository.save(venta);
                        log.info("Venta anulada porque no quedan detalles");
                    }

                } else {
                    // Devolución total
                    log.info("Devolución TOTAL. Se anulará la venta ID: {}", venta.getId());
                    for (VentaDetalle d : venta.getDetalles()) {
                        log.info("Devolviendo stock al lote {} (cantidad {})", d.getLote().getId(), d.getCantidad());
                        loteService.agregarStock(
                                d.getLote().getId(),
                                d.getCantidad(),
                                "Devolución total aprobada - Venta " + venta.getNumeroFactura()
                        );
                    }
                    venta.setEstado(EstadoVenta.ANULADA);
                    ventaRepository.save(venta);
                    log.info("Venta marcada como ANULADA. Nuevo estado: {}", venta.getEstado());
                }
                devolucion.setEstado(EstadoDevolucion.APROBADA);
            } else if ("RECHAZAR".equalsIgnoreCase(request.getAccion())) {
                log.info("Devolución RECHAZADA");
                devolucion.setEstado(EstadoDevolucion.RECHAZADA);
            } else {
                throw new RuntimeException("Acción no válida: " + request.getAccion());
            }

            devolucion.setAdmin(admin);
            devolucion.setObservacionAdmin(request.getObservacion());
            devolucion.setFechaAprobacion(LocalDateTime.now());
            devolucion = devolucionRepository.save(devolucion);
            log.info("Devolución guardada, estado final: {}", devolucion.getEstado());

            return mapToResponseDTO(devolucion);
        } catch (Exception e) {
            log.error("Error al procesar devolución: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void recalcularTotalesVenta(Venta venta) {
        BigDecimal subtotal = venta.getDetalles().stream()
                .map(VentaDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        venta.setSubtotal(subtotal);

        BigDecimal descuentoGlobal = venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO;
        BigDecimal baseImponible = subtotal.subtract(descuentoGlobal);
        BigDecimal impuesto = baseImponible.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        venta.setImpuesto(impuesto);

        BigDecimal total = baseImponible.add(impuesto);
        venta.setTotal(total);

        BigDecimal ganancia = venta.getDetalles().stream()
                .map(VentaDetalle::getGanancia)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        venta.setGanancia(ganancia);

        ventaRepository.save(venta);
    }

    public DevolucionResponseDTO obtenerPorId(Long id) {
        Devolucion dev = devolucionRepository.findByIdWithAll(id)
                .orElseThrow(() -> new RuntimeException("Devolución no encontrada"));
        return mapToResponseDTO(dev);
    }

    public List<DevolucionResponseDTO> listarTodas() {
        return devolucionRepository.findAllWithAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<DevolucionResponseDTO> listarPendientes() {
        return devolucionRepository.findByEstadoWithAll(EstadoDevolucion.PENDIENTE)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<DevolucionResponseDTO> listarPorVendedor(Long vendedorId) {
        Usuario vendedor = usuarioRepository.findById(vendedorId)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));
        return devolucionRepository.findByVendedorWithAll(vendedor).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public Devolucion obtenerDevolucionEntity(Long id) {
        return devolucionRepository.findByIdWithAll(id)
                .orElseThrow(() -> new RuntimeException("Devolución no encontrada"));
    }

    public Devolucion obtenerDevolucionConVentaYDetalles(Long id) {
        return devolucionRepository.findByIdWithVentaAndDetalles(id)
                .orElseThrow(() -> new RuntimeException("Devolución no encontrada"));
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
    public Devolucion obtenerDevolucionParaPdf(Long id) {
        return devolucionRepository.findByIdParaPdf(id)
                .orElseThrow(() -> new RuntimeException("Devolución no encontrada"));
    }
}