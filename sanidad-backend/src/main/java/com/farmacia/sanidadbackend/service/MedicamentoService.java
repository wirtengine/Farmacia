package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.dto.MedicamentoRequest;
import com.farmacia.sanidadbackend.dto.MedicamentoResponse;
import com.farmacia.sanidadbackend.model.Medicamento;
import com.farmacia.sanidadbackend.repository.MedicamentoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;

    @Transactional
    public MedicamentoResponse crearMedicamento(MedicamentoRequest request) {

        if (medicamentoRepository.existsByRegistroSanitario(request.getRegistroSanitario())) {
            throw new IllegalArgumentException("Ya existe un medicamento con ese registro sanitario");
        }

        Medicamento medicamento = mapToEntity(new Medicamento(), request);
        medicamento.setActivo(true);

        Medicamento guardado = medicamentoRepository.save(medicamento);
        return MedicamentoResponse.fromEntity(guardado);
    }

    @Transactional(readOnly = true)
    public List<MedicamentoResponse> listarActivos() {
        return medicamentoRepository.findByActivoTrue()
                .stream()
                .map(MedicamentoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicamentoResponse obtenerPorId(Long id) {

        Medicamento medicamento = medicamentoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicamento no encontrado con id: " + id));

        return MedicamentoResponse.fromEntity(medicamento);
    }

    @Transactional
    public MedicamentoResponse actualizarMedicamento(Long id, MedicamentoRequest request) {

        Medicamento medicamento = medicamentoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicamento no encontrado con id: " + id));

        if (!medicamento.getRegistroSanitario().equals(request.getRegistroSanitario())
                && medicamentoRepository.existsByRegistroSanitario(request.getRegistroSanitario())) {
            throw new IllegalArgumentException("Ya existe otro medicamento con ese registro sanitario");
        }

        mapToEntity(medicamento, request);

        Medicamento actualizado = medicamentoRepository.save(medicamento);
        return MedicamentoResponse.fromEntity(actualizado);
    }

    @Transactional
    public void desactivarMedicamento(Long id) {

        Medicamento medicamento = medicamentoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicamento no encontrado con id: " + id));

        medicamento.setActivo(false);
        medicamentoRepository.save(medicamento);
    }

    @Transactional
    public void activarMedicamento(Long id) {

        Medicamento medicamento = medicamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicamento no encontrado con id: " + id));

        if (!Boolean.TRUE.equals(medicamento.getActivo())) {
            medicamento.setActivo(true);
            medicamentoRepository.save(medicamento);
        }
    }

    private Medicamento mapToEntity(Medicamento medicamento, MedicamentoRequest request) {

        medicamento.setRegistroSanitario(request.getRegistroSanitario());
        medicamento.setNombre(request.getNombre());
        medicamento.setPresentacion(request.getPresentacion());
        medicamento.setVia(request.getVia());
        medicamento.setFabricante(request.getFabricante());
        medicamento.setTipoVenta(request.getTipoVenta());
        medicamento.setPrecioUnitario(request.getPrecioUnitario());
        medicamento.setReceta(Boolean.TRUE.equals(request.getReceta()));

        return medicamento;
    }
}