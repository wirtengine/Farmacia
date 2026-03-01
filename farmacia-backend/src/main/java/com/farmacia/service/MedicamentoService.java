package com.farmacia.service;

import com.farmacia.dto.MedicamentoRequestDTO;
import com.farmacia.dto.MedicamentoResponseDTO;
import com.farmacia.model.Medicamento;
import com.farmacia.repository.MedicamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicamentoService {

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    // Crear medicamento
    @Transactional
    public MedicamentoResponseDTO crearMedicamento(MedicamentoRequestDTO request) {
        // Validar que el registro sanitario no exista
        if (medicamentoRepository.findByRegistroSanitario(request.getRegistroSanitario()).isPresent()) {
            throw new RuntimeException("Ya existe un medicamento con ese registro sanitario");
        }

        Medicamento medicamento = new Medicamento();
        medicamento.setNombre(request.getNombre());
        medicamento.setPrincipioActivo(request.getPrincipioActivo());
        medicamento.setPresentacion(request.getPresentacion());
        medicamento.setViaAdministracion(request.getViaAdministracion());
        medicamento.setFabricante(request.getFabricante());
        medicamento.setRegistroSanitario(request.getRegistroSanitario());
        medicamento.setRequiereReceta(request.getRequiereReceta());
        medicamento.setTipoVenta(request.getTipoVenta());
        medicamento.setPrecioVenta(request.getPrecioVenta());
        medicamento.setStockMinimo(request.getStockMinimo());
        medicamento.setStockMaximo(request.getStockMaximo());
        medicamento.setActivo(true);

        Medicamento guardado = medicamentoRepository.save(medicamento);
        return mapToResponseDTO(guardado);
    }

    // Listar todos los medicamentos activos
    public List<MedicamentoResponseDTO> listarActivos() {
        return medicamentoRepository.findByActivoTrue()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // Obtener un medicamento por id
    public MedicamentoResponseDTO obtenerPorId(Long id) {
        Medicamento medicamento = medicamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicamento no encontrado"));
        return mapToResponseDTO(medicamento);
    }

    // Actualizar medicamento
    @Transactional
    public MedicamentoResponseDTO actualizarMedicamento(Long id, MedicamentoRequestDTO request) {
        Medicamento medicamento = medicamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicamento no encontrado"));

        // Si se cambia el registro sanitario, verificar que no exista otro
        if (!medicamento.getRegistroSanitario().equals(request.getRegistroSanitario())) {
            if (medicamentoRepository.findByRegistroSanitario(request.getRegistroSanitario()).isPresent()) {
                throw new RuntimeException("Ya existe otro medicamento con ese registro sanitario");
            }
        }

        medicamento.setNombre(request.getNombre());
        medicamento.setPrincipioActivo(request.getPrincipioActivo());
        medicamento.setPresentacion(request.getPresentacion());
        medicamento.setViaAdministracion(request.getViaAdministracion());
        medicamento.setFabricante(request.getFabricante());
        medicamento.setRegistroSanitario(request.getRegistroSanitario());
        medicamento.setRequiereReceta(request.getRequiereReceta());
        medicamento.setTipoVenta(request.getTipoVenta());
        medicamento.setPrecioVenta(request.getPrecioVenta());
        medicamento.setStockMinimo(request.getStockMinimo());
        medicamento.setStockMaximo(request.getStockMaximo());

        Medicamento actualizado = medicamentoRepository.save(medicamento);
        return mapToResponseDTO(actualizado);
    }

    // Eliminación lógica (desactivar)
    @Transactional
    public void desactivarMedicamento(Long id) {
        Medicamento medicamento = medicamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicamento no encontrado"));
        medicamento.setActivo(false);
        medicamentoRepository.save(medicamento);
    }

    // Método privado para mapear de entidad a DTO
    private MedicamentoResponseDTO mapToResponseDTO(Medicamento medicamento) {
        MedicamentoResponseDTO dto = new MedicamentoResponseDTO();
        dto.setId(medicamento.getId());
        dto.setNombre(medicamento.getNombre());
        dto.setPrincipioActivo(medicamento.getPrincipioActivo());
        dto.setPresentacion(medicamento.getPresentacion());
        dto.setViaAdministracion(medicamento.getViaAdministracion());
        dto.setFabricante(medicamento.getFabricante());
        dto.setRegistroSanitario(medicamento.getRegistroSanitario());
        dto.setRequiereReceta(medicamento.getRequiereReceta());
        dto.setTipoVenta(medicamento.getTipoVenta());
        dto.setPrecioVenta(medicamento.getPrecioVenta());
        dto.setStockMinimo(medicamento.getStockMinimo());
        dto.setStockMaximo(medicamento.getStockMaximo());
        dto.setActivo(medicamento.getActivo());
        return dto;
    }
}