package com.farmacia.service;

import com.farmacia.dto.*;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VentaService {

    private static final Logger log = LoggerFactory.getLogger(VentaService.class);

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Autowired
    private LoteService loteService;

    private static final BigDecimal IMPUESTO = new BigDecimal("0.15");

    @Transactional
    public VentaResponseDTO crearVenta(VentaRequestDTO request) {
        Usuario vendedor = getCurrentUser();

        Cliente cliente = null;
        if (request.getClienteId() != null) {
            cliente = clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        }

        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setVendedor(vendedor);
        venta.setDescuento(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO);
        venta.setEstado(EstadoVenta.CONFIRMADA);

        List<VentaDetalle> detalles = procesarDetalles(request.getDetalles(), venta);
        venta.setDetalles(detalles);

        BigDecimal subtotal = detalles.stream()
                .map(VentaDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        venta.setSubtotal(subtotal);

        BigDecimal descuentoGlobal = venta.getDescuento();
        BigDecimal baseImponible = subtotal.subtract(descuentoGlobal);
        BigDecimal impuesto = baseImponible.multiply(IMPUESTO).setScale(2, RoundingMode.HALF_UP);
        venta.setImpuesto(impuesto);

        BigDecimal total = baseImponible.add(impuesto);
        venta.setTotal(total);

        BigDecimal ganancia = detalles.stream()
                .map(VentaDetalle::getGanancia)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        venta.setGanancia(ganancia);

        venta.setNumeroFactura(generarNumeroFactura());

        venta = ventaRepository.save(venta);
        log.info("Venta creada: ID={}, factura={}", venta.getId(), venta.getNumeroFactura());
        return mapToResponseDTO(venta);
    }

    private List<VentaDetalle> procesarDetalles(List<DetalleVentaRequestDTO> detallesRequest, Venta venta) {
        List<VentaDetalle> todosDetalles = new ArrayList<>();

        for (DetalleVentaRequestDTO detReq : detallesRequest) {
            Medicamento medicamento = medicamentoRepository.findById(detReq.getMedicamentoId())
                    .orElseThrow(() -> new RuntimeException("Medicamento no encontrado: " + detReq.getMedicamentoId()));

            int cantidadSolicitada = detReq.getCantidad();
            // 🔽 LÍNEA CORREGIDA 🔽
            List<Lote> lotes = loteRepository.findByMedicamentoAndActivoTrueAndCantidadActualGreaterThanWithMedicamento(medicamento, 0);

            if (lotes.isEmpty()) {
                throw new RuntimeException("No hay lotes disponibles para " + medicamento.getNombre());
            }

            int cantidadRestante = cantidadSolicitada;
            BigDecimal descuentoItem = detReq.getDescuento() != null ? detReq.getDescuento() : BigDecimal.ZERO;

            for (Lote lote : lotes) {
                if (cantidadRestante <= 0) break;

                int cantidadTomar = Math.min(cantidadRestante, lote.getCantidadActual());
                loteService.descontarStock(lote.getId(), cantidadTomar, "Venta #" + venta.getNumeroFactura());

                VentaDetalle detalle = new VentaDetalle();
                detalle.setVenta(venta);
                detalle.setLote(lote);
                detalle.setMedicamento(medicamento);
                detalle.setCantidad(cantidadTomar);
                detalle.setPrecioUnitario(medicamento.getPrecioVenta());

                BigDecimal costo = lote.getPrecioCompra() != null ? lote.getPrecioCompra() : BigDecimal.ZERO;
                detalle.setCostoUnitario(costo);
                detalle.setDescuento(descuentoItem);

                BigDecimal subtotalItem = medicamento.getPrecioVenta()
                        .multiply(new BigDecimal(cantidadTomar))
                        .subtract(descuentoItem);
                detalle.setSubtotal(subtotalItem);

                BigDecimal gananciaItem = medicamento.getPrecioVenta()
                        .subtract(costo)
                        .multiply(new BigDecimal(cantidadTomar))
                        .subtract(descuentoItem);
                detalle.setGanancia(gananciaItem);

                todosDetalles.add(detalle);
                cantidadRestante -= cantidadTomar;
            }

            if (cantidadRestante > 0) {
                throw new RuntimeException("Stock insuficiente para " + medicamento.getNombre() + ". Faltan " + cantidadRestante + " unidades.");
            }
        }
        return todosDetalles;
    }

    private String generarNumeroFactura() {
        String prefijo = "F-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy")) + "-";
        List<String> facturas = ventaRepository.findUltimoNumeroFacturaByPrefijo(prefijo);
        int correlativo = 1;
        if (!facturas.isEmpty()) {
            String ultimo = facturas.get(0);
            correlativo = Integer.parseInt(ultimo.substring(prefijo.length())) + 1;
        }
        return prefijo + String.format("%05d", correlativo);
    }

    private Usuario getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        }
        throw new RuntimeException("Usuario no autenticado");
    }

    public VentaResponseDTO obtenerVenta(Long id) {
        Venta venta = ventaRepository.findByIdWithAll(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        return mapToResponseDTO(venta);
    }

    public List<VentaResponseDTO> listarVentas() {
        return ventaRepository.findAllNoAnuladasWithFetch().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<VentaResponseDTO> listarVentasPorVendedor(Long vendedorId) {
        Usuario vendedor = usuarioRepository.findById(vendedorId)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));
        return ventaRepository.findByVendedorWithFetch(vendedor).stream()
                .filter(v -> v.getEstado() != EstadoVenta.ANULADA)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<VentaResponseDTO> listarVentasPorCliente(Long clienteId) {
        return ventaRepository.findByClienteIdWithFetch(clienteId).stream()
                .filter(v -> v.getEstado() != EstadoVenta.ANULADA)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private VentaResponseDTO mapToResponseDTO(Venta venta) {
        VentaResponseDTO dto = new VentaResponseDTO();
        dto.setId(venta.getId());
        dto.setNumeroFactura(venta.getNumeroFactura());
        if (venta.getCliente() != null) {
            dto.setClienteId(venta.getCliente().getId());
            dto.setClienteNombre(venta.getCliente().getNombreCompleto());
        }
        dto.setVendedorId(venta.getVendedor().getId());
        dto.setVendedorNombre(venta.getVendedor().getNombre() + " " + venta.getVendedor().getApellido());
        dto.setFecha(venta.getFecha());
        dto.setSubtotal(venta.getSubtotal());
        dto.setDescuento(venta.getDescuento());
        dto.setImpuesto(venta.getImpuesto());
        dto.setTotal(venta.getTotal());
        dto.setGanancia(venta.getGanancia());
        dto.setEstado(venta.getEstado().toString());
        dto.setDetalles(venta.getDetalles().stream().map(this::mapDetalleToDTO).collect(Collectors.toList()));
        return dto;
    }

    private VentaDetalleResponseDTO mapDetalleToDTO(VentaDetalle detalle) {
        VentaDetalleResponseDTO dto = new VentaDetalleResponseDTO();
        dto.setId(detalle.getId());
        dto.setLoteId(detalle.getLote().getId());
        dto.setNumeroLote(detalle.getLote().getNumeroLote());
        dto.setMedicamentoId(detalle.getMedicamento().getId());
        dto.setMedicamentoNombre(detalle.getMedicamento().getNombre());
        dto.setPresentacion(detalle.getMedicamento().getPresentacion());
        dto.setCantidad(detalle.getCantidad());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setCostoUnitario(detalle.getCostoUnitario());
        dto.setDescuento(detalle.getDescuento());
        dto.setSubtotal(detalle.getSubtotal());
        dto.setGanancia(detalle.getGanancia());
        return dto;
    }
}