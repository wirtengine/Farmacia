package com.farmacia.sanidadbackend.inteligencia.perdidas;

import com.farmacia.sanidadbackend.inteligencia.perdidas.dto.*;
import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerdidasService {

    private final LoteRepository loteRepository;
    private final LoteDetalleRepository loteDetalleRepository;
    private final UbicacionLoteRepository ubicacionLoteRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final MedicamentoRepository medicamentoRepository;

    private static final int DIAS_INMOVIL = 90;

    @Transactional(readOnly = true)
    public List<ProductoVencidoDTO> obtenerProductosVencidos() {
        LocalDate hoy = LocalDate.now();
        List<Object[]> resultados = loteRepository.findLotesVencidosConStock(hoy);
        List<ProductoVencidoDTO> lista = new ArrayList<>();

        for (Object[] row : resultados) {
            Lote lote = (Lote) row[0];
            // Cada detalle del lote puede tener un medicamento diferente
            for (LoteDetalle det : lote.getDetalles()) {
                if (det.getCantidad() > 0) {
                    ProductoVencidoDTO dto = new ProductoVencidoDTO();
                    dto.setLoteId(lote.getId());
                    dto.setNumeroLote(lote.getNumeroLote());
                    dto.setFechaVencimiento(lote.getFechaVencimiento());
                    dto.setMedicamentoId(det.getMedicamento().getId());
                    dto.setMedicamentoNombre(det.getMedicamento().getNombre());
                    dto.setCantidadVencida(det.getCantidad());
                    dto.setValorPerdido(det.getMedicamento().getPrecioUnitario()
                            .multiply(BigDecimal.valueOf(det.getCantidad())));
                    lista.add(dto);
                }
            }
        }
        return lista;
    }

    @Transactional(readOnly = true)
    public List<ProductoInmovilDTO> obtenerProductosInmoviles() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(DIAS_INMOVIL);
        // Ventas por medicamento en el período
        List<Object[]> ventasPorMedicamento = ventaDetalleRepository.sumVentasPorMedicamentoDesde(fechaLimite);
        Map<Long, Integer> ventasMap = ventasPorMedicamento.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        // Stock actual por medicamento
        LocalDate hoy = LocalDate.now();
        List<Object[]> stockResult = loteDetalleRepository.findStockActualPorMedicamento(hoy);
        Map<Long, Integer> stockMap = stockResult.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        List<ProductoInmovilDTO> inmoviles = new ArrayList<>();
        List<Medicamento> medicamentos = medicamentoRepository.findByActivoTrue();

        for (Medicamento m : medicamentos) {
            Integer ventasProducto = ventasMap.getOrDefault(m.getId(), 0);
            Integer stock = stockMap.getOrDefault(m.getId(), 0);
            if (stock > 0 && ventasProducto == 0) {
                ProductoInmovilDTO dto = new ProductoInmovilDTO();
                dto.setMedicamentoId(m.getId());
                dto.setMedicamentoNombre(m.getNombre());
                dto.setStockActual(stock);
                dto.setVentasUltimos90Dias(0);
                dto.setDiasSinMovimiento(DIAS_INMOVIL);
                dto.setValorInmovilizado(m.getPrecioUnitario().multiply(BigDecimal.valueOf(stock)));
                inmoviles.add(dto);
            }
        }
        return inmoviles;
    }

    @Transactional(readOnly = true)
    public List<InconsistenciaStockDTO> obtenerInconsistenciasStock() {
        // Suma de stock en ubicaciones por loteDetalle
        List<Object[]> ubicacionesSum = ubicacionLoteRepository.sumCantidadPorLoteDetalle();
        Map<Long, Integer> ubicacionesMap = ubicacionesSum.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        List<LoteDetalle> todosDetalles = loteDetalleRepository.findAll();
        List<InconsistenciaStockDTO> inconsistencias = new ArrayList<>();

        for (LoteDetalle ld : todosDetalles) {
            int cantLote = ld.getCantidad();
            int cantUbic = ubicacionesMap.getOrDefault(ld.getId(), 0);
            if (cantLote != cantUbic) {
                InconsistenciaStockDTO dto = new InconsistenciaStockDTO();
                dto.setLoteDetalleId(ld.getId());
                dto.setMedicamentoId(ld.getMedicamento().getId());
                dto.setMedicamentoNombre(ld.getMedicamento().getNombre());
                dto.setCantidadLote(cantLote);
                dto.setCantidadUbicaciones(cantUbic);
                dto.setDiferencia(cantLote - cantUbic);
                inconsistencias.add(dto);
            }
        }
        return inconsistencias;
    }

    @Transactional(readOnly = true)
    public ResumenPerdidasDTO obtenerResumenPerdidas() {
        ResumenPerdidasDTO resumen = new ResumenPerdidasDTO();

        List<ProductoVencidoDTO> vencidos = obtenerProductosVencidos();
        BigDecimal totalVencido = vencidos.stream()
                .map(ProductoVencidoDTO::getValorPerdido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resumen.setTotalPerdidasVencimiento(totalVencido);
        resumen.setCantidadProductosVencidos(vencidos.size());

        List<ProductoInmovilDTO> inmoviles = obtenerProductosInmoviles();
        BigDecimal totalInmovil = inmoviles.stream()
                .map(ProductoInmovilDTO::getValorInmovilizado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resumen.setTotalInmovilizado(totalInmovil);
        resumen.setCantidadProductosInmoviles(inmoviles.size());

        List<InconsistenciaStockDTO> inconsistencias = obtenerInconsistenciasStock();
        resumen.setCantidadInconsistencias(inconsistencias.size());

        return resumen;
    }
}