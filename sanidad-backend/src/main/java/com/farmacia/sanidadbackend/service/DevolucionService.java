package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.*;
import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class DevolucionService {

    private final DevolucionRepository devolucionRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteDetalleRepository loteDetalleRepository;
    private final ClienteService clienteService;

    private static final BigDecimal IVA = new BigDecimal("0.15");

    private String generarNumeroDevolucion() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String numero;
        do {
            int random = ThreadLocalRandom.current().nextInt(1000, 10000);
            numero = "DEV-" + fecha + "-" + random;
        } while (devolucionRepository.existsByNumeroDevolucion(numero));
        return numero;
    }

    public DevolucionResponse solicitarDevolucion(DevolucionRequest request) {

        Venta venta = ventaRepository.findByIdAndActivoTrue(request.getVentaId())
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada"));

        Usuario solicitante = usuarioRepository.findById(request.getSolicitadoPorId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Devolucion devolucion = new Devolucion();
        devolucion.setVenta(venta);
        devolucion.setSolicitadoPor(solicitante);
        devolucion.setEstado(EstadoDevolucion.PENDIENTE);
        devolucion.setMotivo(request.getMotivo());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<DevolucionDetalle> detalles = new ArrayList<>();

        for (DevolucionDetalleRequest detReq : request.getDetalles()) {

            VentaDetalle ventaDetalle = venta.getDetalles().stream()
                    .filter(vd -> vd.getId().equals(detReq.getVentaDetalleId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Detalle de venta no encontrado"));

            if (detReq.getCantidadDevuelta() > ventaDetalle.getCantidad()) {
                throw new IllegalArgumentException("Cantidad devuelta excede la vendida");
            }

            BigDecimal precio = ventaDetalle.getPrecioUnitario();
            BigDecimal subtotalDetalle = precio.multiply(BigDecimal.valueOf(detReq.getCantidadDevuelta()));

            DevolucionDetalle detalle = new DevolucionDetalle();
            detalle.setDevolucion(devolucion);
            detalle.setVentaDetalle(ventaDetalle);
            detalle.setLoteDetalle(ventaDetalle.getLoteDetalle());
            detalle.setCantidadDevuelta(detReq.getCantidadDevuelta());
            detalle.setPrecioUnitario(precio);
            detalle.setSubtotal(subtotalDetalle);

            detalles.add(detalle);
            subtotal = subtotal.add(subtotalDetalle);
        }

        BigDecimal ivaDevuelto = subtotal.multiply(IVA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDevuelto = subtotal.add(ivaDevuelto).setScale(2, RoundingMode.HALF_UP);

        devolucion.setSubtotalDevuelto(subtotal);
        devolucion.setIvaDevuelto(ivaDevuelto);
        devolucion.setTotalDevuelto(totalDevuelto);
        devolucion.setDetalles(detalles);

        Devolucion saved = devolucionRepository.save(devolucion);
        return mapToResponse(saved);
    }

    public DevolucionResponse aprobarDevolucion(DevolucionAprobarRequest request) {

        Devolucion devolucion = devolucionRepository.findById(request.getDevolucionId())
                .orElseThrow(() -> new EntityNotFoundException("Devolución no encontrada"));

        if (devolucion.getEstado() != EstadoDevolucion.PENDIENTE) {
            throw new IllegalStateException("La devolución ya fue procesada");
        }

        Usuario admin = usuarioRepository.findById(request.getAprobadoPorId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        // Rechazo
        if (Boolean.FALSE.equals(request.getAprobada())) {
            devolucion.setEstado(EstadoDevolucion.RECHAZADA);
            devolucion.setMotivo(request.getMotivoRechazo()); // Sobrescribe el motivo original (o puedes usar otro campo)
            devolucion.setAprobadoPor(admin);
            devolucion.setFechaAprobacion(LocalDateTime.now());
            return mapToResponse(devolucionRepository.save(devolucion));
        }

        // Aprobación
        devolucion.setEstado(EstadoDevolucion.APROBADA);
        devolucion.setNumeroDevolucion(generarNumeroDevolucion());
        devolucion.setAprobadoPor(admin);
        devolucion.setFechaAprobacion(LocalDateTime.now());

        // Reponer stock
        for (DevolucionDetalle det : devolucion.getDetalles()) {
            LoteDetalle loteDetalle = det.getLoteDetalle();
            loteDetalle.setCantidad(loteDetalle.getCantidad() + det.getCantidadDevuelta());
            loteDetalleRepository.save(loteDetalle);
        }

        Venta venta = devolucion.getVenta();
        BigDecimal totalVenta = venta.getTotal();
        BigDecimal totalDevuelto = devolucion.getTotalDevuelto();

        // Proporción de devolución
        BigDecimal factor = totalDevuelto.divide(totalVenta, 10, RoundingMode.HALF_UP);

        BigDecimal montoSaldo = venta.getMontoUsadoSaldo() != null ? venta.getMontoUsadoSaldo() : BigDecimal.ZERO;
        BigDecimal montoEfectivo = venta.getMontoEfectivo() != null ? venta.getMontoEfectivo() : BigDecimal.ZERO;

        BigDecimal saldoDevuelto = montoSaldo.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal efectivoDevuelto = montoEfectivo.multiply(factor).setScale(2, RoundingMode.HALF_UP);

        devolucion.setMontoDevueltoSaldo(saldoDevuelto);
        devolucion.setMontoDevueltoEfectivo(efectivoDevuelto);

        // Devolver saldo al cliente
        if (venta.getCliente() != null && saldoDevuelto.compareTo(BigDecimal.ZERO) > 0) {
            clienteService.abonarSaldo(venta.getCliente().getId(), saldoDevuelto);
        }

        // Si se devolvieron todos los productos, desactivar la venta
        boolean devolucionCompleta = devolucion.getDetalles().stream()
                .allMatch(d -> d.getCantidadDevuelta().equals(d.getVentaDetalle().getCantidad()));

        if (devolucionCompleta) {
            venta.setActivo(false);
            ventaRepository.save(venta);
        }

        Devolucion saved = devolucionRepository.save(devolucion);
        return mapToResponse(saved);
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