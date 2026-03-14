package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.*;
import com.farmacia.sanidadbackend.model.Lote;
import com.farmacia.sanidadbackend.model.LoteDetalle;
import com.farmacia.sanidadbackend.model.Medicamento;
import com.farmacia.sanidadbackend.model.Proveedor;
import com.farmacia.sanidadbackend.repository.LoteRepository;
import com.farmacia.sanidadbackend.repository.MedicamentoRepository;
import com.farmacia.sanidadbackend.repository.ProveedorRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class LoteService {

    private final LoteRepository loteRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final ProveedorRepository proveedorRepository;

    private String generarNumeroLote() {

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String numero;

        do {
            int random = ThreadLocalRandom.current().nextInt(1000, 10000);
            numero = "LOTE-" + fecha + "-" + random;
        } while (loteRepository.existsByNumeroLote(numero));

        return numero;
    }

    @Transactional
    public LoteResponse crearLote(LoteRequest request) {

        validarFechas(request.getFechaFabricacion(), request.getFechaVencimiento());

        Proveedor proveedor = proveedorRepository
                .findByIdAndActivoTrue(request.getProveedorId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado o suspendido"));

        Lote lote = new Lote();
        lote.setNumeroLote(generarNumeroLote());
        lote.setFechaFabricacion(request.getFechaFabricacion());
        lote.setFechaVencimiento(request.getFechaVencimiento());
        lote.setProveedor(proveedor);
        lote.setFactura(request.getFactura());
        lote.setActivo(true);

        lote.setDetalles(crearDetalles(request.getDetalles(), lote));

        Lote saved = loteRepository.save(lote);

        return mapToResponse(saved);
    }

    @Transactional
    public LoteResponse actualizarLote(Long id, LoteRequest request) {

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

        // CORRECCIÓN AQUÍ
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