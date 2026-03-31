package com.farmacia.sanidadbackend.inteligencia.ventas;

import com.farmacia.sanidadbackend.dto.SugerenciaProductoDTO;
import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VentaInteligenciaService {

    @Autowired
    private LoteDetalleRepository loteDetalleRepository;

    @Autowired
    private VentaDetalleRepository ventaDetalleRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    private static final int LIMITE_SUGERENCIAS = 5;

    /**
     * Obtiene el lote más próximo a vencer para un medicamento dado.
     */
    public SugerenciaProductoDTO obtenerLoteFIFO(Long medicamentoId) {
        LocalDate hoy = LocalDate.now();
        List<LoteDetalle> lotes = loteDetalleRepository.findLotesDisponiblesPorMedicamentoOrderByVencimiento(medicamentoId, hoy);
        if (lotes.isEmpty()) return null;

        LoteDetalle lote = lotes.get(0);
        SugerenciaProductoDTO dto = new SugerenciaProductoDTO();
        dto.setMedicamentoId(medicamentoId);
        dto.setNombre(lote.getMedicamento().getNombre());
        dto.setPresentacion(lote.getMedicamento().getPresentacion());
        dto.setPrecioUnitario(lote.getMedicamento().getPrecioUnitario());
        dto.setTipoSugerencia("FIFO");
        dto.setMensaje("Lote con fecha de vencimiento más próxima: " + lote.getLote().getFechaVencimiento() +
                ". Cantidad disponible: " + lote.getCantidad());
        dto.setLoteDetalleId(lote.getId());
        dto.setCantidadSugerida(Math.min(lote.getCantidad(), 10)); // sugerir cantidad típica
        return dto;
    }

    /**
     * Sugiere productos complementarios basados en ventas anteriores (market basket).
     */
    public List<SugerenciaProductoDTO> sugerirComplementarios(Long medicamentoId) {
        List<Object[]> resultados = ventaDetalleRepository.findComplementaryProducts(medicamentoId);
        List<SugerenciaProductoDTO> sugerencias = new ArrayList<>();
        for (int i = 0; i < Math.min(resultados.size(), LIMITE_SUGERENCIAS); i++) {
            Object[] row = resultados.get(i);
            SugerenciaProductoDTO dto = new SugerenciaProductoDTO();
            dto.setMedicamentoId(((Number) row[0]).longValue());
            dto.setNombre((String) row[1]);
            dto.setTipoSugerencia("COMPLEMENTARIO");
            dto.setMensaje("Suele comprarse junto con este producto (frecuencia: " + ((Number) row[2]).intValue() + " veces)");
            // Obtener precio del medicamento
            // (podríamos buscar en el repositorio de medicamentos, pero por simplicidad lo dejamos)
            sugerencias.add(dto);
        }
        return sugerencias;
    }

    /**
     * Obtiene el historial de compras del cliente y productos más frecuentes.
     */
    public List<SugerenciaProductoDTO> obtenerContextoCliente(Long clienteId) {
        // Productos más frecuentes del cliente
        List<Object[]> productosFrecuentes = clienteRepository.findTopProductosByCliente(clienteId);
        List<SugerenciaProductoDTO> sugerencias = new ArrayList<>();
        for (int i = 0; i < Math.min(productosFrecuentes.size(), LIMITE_SUGERENCIAS); i++) {
            Object[] row = productosFrecuentes.get(i);
            SugerenciaProductoDTO dto = new SugerenciaProductoDTO();
            dto.setMedicamentoId(((Number) row[0]).longValue());
            dto.setNombre((String) row[1]);
            dto.setTipoSugerencia("FRECUENTE_CLIENTE");
            dto.setMensaje("Producto comprado con frecuencia (" + ((Number) row[2]).intValue() + " unidades totales)");
            sugerencias.add(dto);
        }
        return sugerencias;
    }

    /**
     * Obtiene información completa para la venta guiada (FIFO + complementarios + contexto cliente).
     */
    public VentaGuiadaResponse obtenerInfoVentaGuiada(Long clienteId, Long medicamentoId) {
        VentaGuiadaResponse response = new VentaGuiadaResponse();
        if (medicamentoId != null) {
            response.setLoteRecomendado(obtenerLoteFIFO(medicamentoId));
            response.setComplementarios(sugerirComplementarios(medicamentoId));
        }
        if (clienteId != null) {
            response.setProductosFrecuentesCliente(obtenerContextoCliente(clienteId));
            response.setUltimasCompras(obtenerUltimasComprasCliente(clienteId));
        }
        return response;
    }

    private List<String> obtenerUltimasComprasCliente(Long clienteId) {
        Pageable pageable = PageRequest.of(0, 5);
        List<Venta> ventas = clienteRepository.findLastPurchasesByCliente(clienteId, pageable);
        return ventas.stream()
                .map(v -> v.getNumeroFactura() + " - " + v.getFecha().toLocalDate())
                .collect(Collectors.toList());
    }
}