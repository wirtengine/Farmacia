package com.farmacia.service;

import com.farmacia.dto.LoteRequestDTO;
import com.farmacia.dto.LoteResponseDTO;
import com.farmacia.model.Lote;
import com.farmacia.model.Medicamento;
import com.farmacia.model.MovimientoInventario;
import com.farmacia.model.Usuario;
import com.farmacia.repository.LoteRepository;
import com.farmacia.repository.MedicamentoRepository;
import com.farmacia.repository.MovimientoInventarioRepository;
import com.farmacia.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoteService {

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ================================
    // CREAR LOTE (ENTRADA INVENTARIO)
    // ================================
    @Transactional
    public LoteResponseDTO crearLote(LoteRequestDTO request) {
        Medicamento medicamento = medicamentoRepository.findById(request.getMedicamentoId())
                .orElseThrow(() -> new RuntimeException("Medicamento no encontrado"));

        if (loteRepository.findByNumeroLote(request.getNumeroLote()).isPresent()) {
            throw new RuntimeException("Ya existe un lote con ese número");
        }

        if (request.getFechaVencimiento().isBefore(request.getFechaFabricacion())) {
            throw new RuntimeException("La fecha de vencimiento no puede ser anterior a la de fabricación");
        }

        Lote lote = new Lote();
        lote.setNumeroLote(request.getNumeroLote());
        lote.setMedicamento(medicamento);
        lote.setFechaFabricacion(request.getFechaFabricacion());
        lote.setFechaVencimiento(request.getFechaVencimiento());
        lote.setCantidadInicial(request.getCantidadInicial());
        lote.setCantidadActual(request.getCantidadInicial());
        lote.setFabricante(request.getFabricante());
        lote.setProveedor(request.getProveedor());
        lote.setFechaIngreso(LocalDateTime.now());
        lote.setActivo(true);

        loteRepository.save(lote);

        registrarMovimiento(lote, "ENTRADA", request.getCantidadInicial(), "Ingreso de lote por compra");

        return convertToResponseDTO(lote);
    }

    // ================================
    // AGREGAR STOCK
    // ================================
    @Transactional
    public void agregarStock(Long loteId, int cantidad, String motivo) {
        if (cantidad <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a cero");
        }

        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));

        lote.setCantidadActual(lote.getCantidadActual() + cantidad);

        if (lote.getCantidadActual() > 0) {
            lote.setActivo(true);
        }

        loteRepository.save(lote);

        registrarMovimiento(lote, "ENTRADA", cantidad, motivo);
    }

    // ================================
    // OTROS MÉTODOS DEL SERVICIO
    // ================================
    @Transactional
    public void descontarStock(Long loteId, int cantidad, String motivo) {
        if (cantidad <= 0) throw new RuntimeException("La cantidad debe ser mayor a cero");

        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));

        if (!lote.getActivo()) throw new RuntimeException("El lote está inactivo");

        if (lote.getCantidadActual() < cantidad)
            throw new RuntimeException("Stock insuficiente en el lote " + lote.getNumeroLote());

        lote.setCantidadActual(lote.getCantidadActual() - cantidad);

        if (lote.getCantidadActual() == 0) lote.setActivo(false);

        loteRepository.save(lote);

        registrarMovimiento(lote, "SALIDA", cantidad, motivo);
    }

    @Transactional
    public void desactivarLote(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));
        lote.setActivo(false);
        loteRepository.save(lote);
    }

    public LoteResponseDTO obtenerPorId(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));
        return convertToResponseDTO(lote);
    }

    public List<LoteResponseDTO> listarTodos() {
        return loteRepository.findAll().stream()
                .filter(Lote::getActivo)
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<LoteResponseDTO> listarPorMedicamento(Long medicamentoId) {
        Medicamento medicamento = medicamentoRepository.findById(medicamentoId)
                .orElseThrow(() -> new RuntimeException("Medicamento no encontrado"));

        return loteRepository
                .findByMedicamentoAndActivoTrueOrderByFechaVencimientoAsc(medicamento)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<Lote> obtenerLotesFEFO(Medicamento medicamento) {
        return loteRepository
                .findByMedicamentoAndActivoTrueAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(medicamento, 0);
    }

    private void registrarMovimiento(Lote lote, String tipo, Integer cantidad, String motivo) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setLote(lote);
        movimiento.setMedicamento(lote.getMedicamento());
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setMotivo(motivo);
        movimiento.setFecha(LocalDateTime.now());

        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
                movimiento.setUsuario(usuario);
            }
        } catch (Exception e) {
            // Usuario null si no está autenticado
        }

        movimientoRepository.save(movimiento);
    }

    private LoteResponseDTO convertToResponseDTO(Lote lote) {
        LoteResponseDTO dto = new LoteResponseDTO();
        dto.setId(lote.getId());
        dto.setNumeroLote(lote.getNumeroLote());
        dto.setMedicamentoId(lote.getMedicamento().getId());
        dto.setMedicamentoNombre(lote.getMedicamento().getNombre());
        dto.setMedicamentoPresentacion(lote.getMedicamento().getPresentacion());
        dto.setFechaFabricacion(lote.getFechaFabricacion());
        dto.setFechaVencimiento(lote.getFechaVencimiento());
        dto.setCantidadInicial(lote.getCantidadInicial());
        dto.setCantidadActual(lote.getCantidadActual());
        dto.setFabricante(lote.getFabricante());
        dto.setProveedor(lote.getProveedor());
        dto.setFechaIngreso(lote.getFechaIngreso());
        dto.setActivo(lote.getActivo());

        LocalDate hoy = LocalDate.now();
        if (lote.getFechaVencimiento().isBefore(hoy)) {
            dto.setEstado("VENCIDO");
        } else if (lote.getFechaVencimiento().isBefore(hoy.plusDays(30))) {
            dto.setEstado("PRÓXIMO A VENCER");
        } else {
            dto.setEstado("VIGENTE");
        }

        return dto;
    }
}