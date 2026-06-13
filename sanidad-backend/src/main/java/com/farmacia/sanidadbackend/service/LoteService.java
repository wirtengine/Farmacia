package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.*;
import com.farmacia.sanidadbackend.model.Lote;
import com.farmacia.sanidadbackend.model.LoteDetalle;
import com.farmacia.sanidadbackend.model.Medicamento;
import com.farmacia.sanidadbackend.model.Proveedor;
import com.farmacia.sanidadbackend.repository.LoteRepository;
import com.farmacia.sanidadbackend.repository.MedicamentoRepository;
import com.farmacia.sanidadbackend.repository.ProveedorRepository;
import com.farmacia.sanidadbackend.repository.VentaDetalleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoteService {

    private final JdbcTemplate jdbcTemplate;
    private final LoteRepository loteRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final ProveedorRepository proveedorRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final UbicacionService ubicacionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Crea un lote con sus detalles usando la función fn_crear_lote.
     */
    @Transactional
    public LoteResponse crearLote(LoteRequest request) {
        // Validación de fechas (la función también lo hará, pero fallamos rápido)
        validarFechas(request.getFechaFabricacion(), request.getFechaVencimiento());

        String detallesJson;
        try {
            detallesJson = objectMapper.writeValueAsString(request.getDetalles());
        } catch (Exception e) {
            throw new RuntimeException("Error al convertir detalles a JSON", e);
        }

        String sql = "SELECT fn_crear_lote(?, ?, ?, ?, ?::jsonb)";
        Long loteId = jdbcTemplate.queryForObject(sql, Long.class,
                request.getFechaFabricacion() != null ? Date.valueOf(request.getFechaFabricacion()) : null,
                request.getFechaVencimiento() != null ? Date.valueOf(request.getFechaVencimiento()) : null,
                request.getProveedorId(),
                request.getFactura(),
                detallesJson
        );

        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado después de crearlo"));
        return mapToResponse(lote);
    }

    // El resto de métodos se mantienen sin cambios
    @Transactional
    public LoteResponse actualizarLote(Long id, LoteRequest request) {
        if (ventaDetalleRepository.existsByLoteId(id)) {
            throw new IllegalStateException("No se puede modificar un lote que ya tiene ventas asociadas. Solo puede desactivarlo.");
        }

        validarFechas(request.getFechaFabricacion(), request.getFechaVencimiento());

        Lote lote = loteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado o suspendido"));

        Proveedor proveedor = proveedorRepository
                .findByIdAndActivoTrue(request.getProveedorId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado o suspendido"));

        lote.setFechaFabricacion(request.getFechaFabricacion());
        lote.setFechaVencimiento(request.getFechaVencimiento());
        lote.setProveedor(proveedor);
        lote.setFactura(request.getFactura());

        lote.getDetalles().clear();
        lote.getDetalles().addAll(crearDetalles(request.getDetalles(), lote));

        Lote updated = loteRepository.save(lote);
        return mapToResponse(updated);
    }

    public List<LoteResponse> listarLotes() {
        return loteRepository.findByActivoTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public LoteResponse obtenerLote(Long id) {
        Lote lote = loteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado o suspendido"));
        return mapToResponse(lote);
    }

    @Transactional
    public void suspenderLote(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado"));
        lote.setActivo(false);
        loteRepository.save(lote);
    }

    private List<LoteDetalle> crearDetalles(List<LoteDetalleRequest> detallesRequest, Lote lote) {
        return detallesRequest.stream()
                .map(detalleReq -> {
                    Medicamento medicamento = medicamentoRepository
                            .findByIdAndActivoTrue(detalleReq.getMedicamentoId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Medicamento no encontrado o suspendido: " + detalleReq.getMedicamentoId()));

                    LoteDetalle detalle = new LoteDetalle();
                    detalle.setMedicamento(medicamento);
                    detalle.setCantidad(detalleReq.getCantidad());
                    detalle.setLote(lote);
                    return detalle;
                })
                .toList();
    }

    private LoteResponse mapToResponse(Lote lote) {
        LoteResponse response = new LoteResponse();

        response.setId(lote.getId());
        response.setNumeroLote(lote.getNumeroLote());
        response.setFechaFabricacion(lote.getFechaFabricacion());
        response.setFechaVencimiento(lote.getFechaVencimiento());

        if (lote.getProveedor() != null) {
            response.setProveedorId(lote.getProveedor().getId());
            response.setProveedorNombre(lote.getProveedor().getNombre());
            response.setProveedorRuc(lote.getProveedor().getRuc());
        }

        response.setFactura(lote.getFactura());
        response.setActivo(lote.isActivo());

        List<LoteDetalleResponse> detalles = lote.getDetalles()
                .stream()
                .map(detalle -> {
                    LoteDetalleResponse dto = new LoteDetalleResponse();
                    dto.setId(detalle.getId());
                    dto.setMedicamentoId(detalle.getMedicamento().getId());
                    dto.setMedicamentoNombre(detalle.getMedicamento().getNombre());
                    dto.setMedicamentoPresentacion(detalle.getMedicamento().getPresentacion());
                    dto.setFabricante(detalle.getMedicamento().getFabricante());
                    dto.setCantidad(detalle.getCantidad());
                    dto.setPrecioUnitario(detalle.getMedicamento().getPrecioUnitario());
                    return dto;
                })
                .toList();

        response.setDetalles(detalles);
        return response;
    }

    private void validarFechas(LocalDate fabricacion, LocalDate vencimiento) {
        if (fabricacion != null && vencimiento != null && vencimiento.isBefore(fabricacion)) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser menor que la fecha de fabricación");
        }
    }
}