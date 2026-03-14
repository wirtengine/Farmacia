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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteDetalleRepository loteDetalleRepository;
    private final ClienteService clienteService;

    private static final BigDecimal IVA_PORCENTAJE = new BigDecimal("0.15");

    private String generarNumeroFactura() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String numero;

        do {
            int random = ThreadLocalRandom.current().nextInt(1000, 10000);
            numero = "F" + fecha + "-" + random;
        } while (ventaRepository.existsByNumeroFactura(numero));

        return numero;
    }

    @Transactional
    public VentaResponse crearVenta(VentaRequest request) {

        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Cliente cliente = null;
        TipoVenta tipo = TipoVenta.RAPIDA;

        if (request.getClienteId() != null) {
            cliente = clienteRepository.findByIdAndActivoTrue(request.getClienteId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

            tipo = TipoVenta.CLIENTE;
        }

        Venta venta = new Venta();
        venta.setNumeroFactura(generarNumeroFactura());
        venta.setFecha(LocalDateTime.now());
        venta.setCliente(cliente);
        venta.setUsuario(usuario);
        venta.setTipo(tipo);
        venta.setActivo(true);

        BigDecimal subtotal = BigDecimal.ZERO;

        for (VentaDetalleRequest detalleReq : request.getDetalles()) {

            LoteDetalle loteDetalle = loteDetalleRepository.findById(detalleReq.getLoteDetalleId())
                    .orElseThrow(() -> new EntityNotFoundException("LoteDetalle no encontrado"));

            if (!loteDetalle.getLote().isActivo()) {
                throw new IllegalStateException("Lote suspendido");
            }

            if (loteDetalle.getLote().getFechaVencimiento() != null &&
                    loteDetalle.getLote().getFechaVencimiento().isBefore(LocalDate.now())) {
                throw new IllegalStateException("Lote vencido");
            }

            if (loteDetalle.getCantidad() < detalleReq.getCantidad()) {
                throw new IllegalStateException("Stock insuficiente");
            }

            loteDetalle.setCantidad(
                    loteDetalle.getCantidad() - detalleReq.getCantidad()
            );

            BigDecimal precioUnitario = loteDetalle.getMedicamento().getPrecioUnitario();
            BigDecimal cantidad = BigDecimal.valueOf(detalleReq.getCantidad());

            BigDecimal subtotalDetalle = precioUnitario.multiply(cantidad);

            VentaDetalle detalle = new VentaDetalle();
            detalle.setVenta(venta);
            detalle.setLoteDetalle(loteDetalle);
            detalle.setCantidad(detalleReq.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(subtotalDetalle);

            venta.getDetalles().add(detalle);

            subtotal = subtotal.add(subtotalDetalle);
        }

        BigDecimal iva = subtotal.multiply(IVA_PORCENTAJE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva).setScale(2, RoundingMode.HALF_UP);

        venta.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        venta.setIva(iva);
        venta.setTotal(total);

        /* -----------------------------
           VALIDACIÓN DE MÉTODO DE PAGO
           ----------------------------- */

        BigDecimal montoSaldo = request.getMontoUsadoSaldo() != null
                ? request.getMontoUsadoSaldo()
                : BigDecimal.ZERO;

        BigDecimal montoEfectivo = request.getMontoEfectivo() != null
                ? request.getMontoEfectivo()
                : BigDecimal.ZERO;

        BigDecimal totalPagado = montoSaldo.add(montoEfectivo);
        BigDecimal cambio = totalPagado.subtract(total);

        if (cambio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El pago es insuficiente");
        }

        venta.setCambio(cambio.setScale(2, RoundingMode.HALF_UP));

        if (cliente != null) {
            if (montoSaldo.compareTo(BigDecimal.ZERO) > 0) {
                clienteService.descontarSaldo(cliente.getId(), montoSaldo);
            }
        } else {
            if (montoSaldo.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("No se puede usar saldo sin seleccionar cliente");
            }
        }

        venta.setMontoUsadoSaldo(montoSaldo);
        venta.setMontoEfectivo(montoEfectivo);

        Venta saved = ventaRepository.save(venta);

        return mapToResponse(saved);
    }

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