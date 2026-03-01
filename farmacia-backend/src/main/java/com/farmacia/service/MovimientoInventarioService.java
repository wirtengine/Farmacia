package com.farmacia.service;

import com.farmacia.dto.MovimientoInventarioDTO;
import com.farmacia.model.MovimientoInventario;
import com.farmacia.repository.MovimientoInventarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovimientoInventarioService {

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    public List<MovimientoInventarioDTO> listarPorLote(Long loteId) {
        return movimientoRepository.findByLoteOrderByFechaDesc(
                movimientoRepository.findById(loteId).get().getLote() // simplificado, mejor inyectar LoteRepository
        ).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MovimientoInventarioDTO> listarPorMedicamento(Long medicamentoId) {
        // Necesitarías MedicamentoRepository
        return null; // Implementar según necesidad
    }

    private MovimientoInventarioDTO convertToDTO(MovimientoInventario mov) {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO();
        dto.setId(mov.getId());
        dto.setLoteId(mov.getLote().getId());
        dto.setNumeroLote(mov.getLote().getNumeroLote());
        dto.setMedicamentoId(mov.getMedicamento().getId());
        dto.setMedicamentoNombre(mov.getMedicamento().getNombre());
        dto.setTipo(mov.getTipo());
        dto.setCantidad(mov.getCantidad());
        dto.setFecha(mov.getFecha());
        dto.setMotivo(mov.getMotivo());
        if (mov.getUsuario() != null) {
            dto.setUsuarioNombre(mov.getUsuario().getNombre() + " " + mov.getUsuario().getApellido());
        }
        return dto;
    }
}