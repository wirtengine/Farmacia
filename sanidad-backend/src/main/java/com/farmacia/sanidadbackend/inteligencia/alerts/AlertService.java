package com.farmacia.sanidadbackend.inteligencia.alerts;

import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AlertService {

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private VentaDetalleRepository ventaDetalleRepository;

    @Autowired
    private LoteDetalleRepository loteDetalleRepository;

    @Autowired
    private AlertRepository alertRepository;

    private static final int DIAS_STOCK_CRITICO = 7;
    private static final int DIAS_LOTE_VENCE = 30;
    private static final int DIAS_SIN_MOVIMIENTO = 90;

    @Transactional
    @Scheduled(cron = "0 0 1 * * ?")
    public void generarAlertas() {
        // Primero resolver alertas viejas que ya no aplican
        resolverAlertasViejas();

        // Luego generar nuevas alertas
        verificarStockCritico();
        verificarLotesProximosVencer();
        verificarProductosSinMovimiento();
    }

    /**
     * Resuelve automáticamente alertas pendientes cuya condición ya no se cumple.
     */
    private void resolverAlertasViejas() {
        List<Alert> pendingAlerts = alertRepository.findByStatusOrderByCreatedAtDesc(AlertStatus.PENDING);

        // Datos necesarios para evaluaciones
        LocalDate hoy = LocalDate.now();
        LocalDateTime limiteSinMovimiento = LocalDateTime.now().minusDays(DIAS_SIN_MOVIMIENTO);
        LocalDateTime desdeVentas = LocalDateTime.now().minusDays(30);
        LocalDateTime hastaVentas = LocalDateTime.now();

        // Mapa de ventas por medicamento (últimos 30 días) para stock crítico
        List<Object[]> ventasResult = ventaDetalleRepository.sumCantidadByMedicamentoEntreFechas(desdeVentas, hastaVentas);
        Map<Long, Integer> ventasTotalesMap = ventasResult.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        // Mapa de stock actual por medicamento
        List<Object[]> stockResult = loteDetalleRepository.findStockActualPorMedicamento(hoy);
        Map<Long, Integer> stockActualMap = stockResult.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        // Lista de medicamentos con ventas recientes (para sin movimiento)
        List<Object[]> ventasRecientes = ventaDetalleRepository.sumCantidadByMedicamentoEntreFechas(limiteSinMovimiento, hastaVentas);
        Map<Long, Integer> ventasRecientesMap = ventasRecientes.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue(),
                        (a, b) -> a + b
                ));

        for (Alert alert : pendingAlerts) {
            boolean shouldResolve = false;

            switch (alert.getType()) {
                case STOCK_CRITICO:
                    Long medId = alert.getRelatedEntityId();
                    int stockActual = stockActualMap.getOrDefault(medId, 0);
                    int ventasTotales = ventasTotalesMap.getOrDefault(medId, 0);
                    if (ventasTotales > 0) {
                        double promedioDiario = ventasTotales / 30.0;
                        int umbral = (int) Math.ceil(promedioDiario * DIAS_STOCK_CRITICO);
                        if (stockActual >= umbral) {
                            shouldResolve = true;
                        }
                    } else {
                        // Si no hay ventas en 30 días, la alerta de stock crítico ya no tiene sentido
                        shouldResolve = true;
                    }
                    break;

                case LOTE_PROXIMO_VENCER:
                    Long loteId = alert.getRelatedEntityId();
                    Lote lote = loteRepository.findById(loteId).orElse(null);
                    if (lote == null || !lote.isActivo() || lote.getFechaVencimiento().isBefore(hoy) ||
                            lote.getFechaVencimiento().isAfter(hoy.plusDays(DIAS_LOTE_VENCE))) {
                        shouldResolve = true;
                    }
                    break;

                case PRODUCTO_SIN_MOVIMIENTO:
                    Long prodId = alert.getRelatedEntityId();
                    boolean haTenidoVentas = ventasRecientesMap.containsKey(prodId);
                    if (haTenidoVentas) {
                        shouldResolve = true;
                    }
                    break;
            }

            if (shouldResolve) {
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setAcknowledgedAt(LocalDateTime.now());
                alertRepository.save(alert);
            }
        }
    }

    private void verificarStockCritico() {
        LocalDate hoy = LocalDate.now();
        List<Object[]> stockResult = loteDetalleRepository.findStockActualPorMedicamento(hoy);
        Map<Long, Integer> stockActualMap = stockResult.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        LocalDateTime hasta = LocalDateTime.now();
        LocalDateTime desde = hasta.minusDays(30);
        List<Object[]> ventasResult = ventaDetalleRepository.sumCantidadByMedicamentoEntreFechas(desde, hasta);
        Map<Long, Integer> ventasTotalesMap = ventasResult.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue()
                ));

        List<Medicamento> medicamentos = medicamentoRepository.findByActivoTrue();

        for (Medicamento m : medicamentos) {
            int stockActual = stockActualMap.getOrDefault(m.getId(), 0);
            int ventasTotales = ventasTotalesMap.getOrDefault(m.getId(), 0);
            if (ventasTotales == 0) continue;

            double promedioDiario = ventasTotales / 30.0;
            int umbral = (int) Math.ceil(promedioDiario * DIAS_STOCK_CRITICO);

            if (stockActual > 0 && stockActual < umbral) {
                if (!alertRepository.existsByTypeAndRelatedEntityIdAndStatus(AlertType.STOCK_CRITICO, m.getId(), AlertStatus.PENDING)) {
                    Alert alert = new Alert();
                    alert.setType(AlertType.STOCK_CRITICO);
                    alert.setSeverity(stockActual < (umbral / 2) ? AlertSeverity.ALTA : AlertSeverity.MEDIA);
                    alert.setTitle("Stock crítico: " + m.getNombre());
                    alert.setDescription(String.format("Stock actual: %d, venta estimada %d días: %d", stockActual, DIAS_STOCK_CRITICO, umbral));
                    alert.setRelatedEntityId(m.getId());
                    alert.setRelatedEntityType("Medicamento");
                    alert.setCreatedAt(LocalDateTime.now());
                    alertRepository.save(alert);
                }
            }
        }
    }

    private void verificarLotesProximosVencer() {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(DIAS_LOTE_VENCE);
        List<Lote> lotes = loteRepository.findByFechaVencimientoBetween(hoy, limite);

        for (Lote lote : lotes) {
            if (!alertRepository.existsByTypeAndRelatedEntityIdAndStatus(AlertType.LOTE_PROXIMO_VENCER, lote.getId(), AlertStatus.PENDING)) {
                String medicamentosNombres = lote.getDetalles().stream()
                        .map(ld -> ld.getMedicamento().getNombre())
                        .distinct()
                        .collect(Collectors.joining(", "));

                Alert alert = new Alert();
                alert.setType(AlertType.LOTE_PROXIMO_VENCER);
                alert.setSeverity(AlertSeverity.MEDIA);
                alert.setTitle("Lote próximo a vencer");
                alert.setDescription(String.format("Lote %s (medicamentos: %s) vence el %s",
                        lote.getNumeroLote(), medicamentosNombres, lote.getFechaVencimiento()));
                alert.setRelatedEntityId(lote.getId());
                alert.setRelatedEntityType("Lote");
                alert.setCreatedAt(LocalDateTime.now());
                alertRepository.save(alert);
            }
        }
    }

    private void verificarProductosSinMovimiento() {
        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_SIN_MOVIMIENTO);
        List<Medicamento> sinMovimiento = medicamentoRepository.findSinMovimientoDesde(limite);

        for (Medicamento m : sinMovimiento) {
            if (!alertRepository.existsByTypeAndRelatedEntityIdAndStatus(AlertType.PRODUCTO_SIN_MOVIMIENTO, m.getId(), AlertStatus.PENDING)) {
                Alert alert = new Alert();
                alert.setType(AlertType.PRODUCTO_SIN_MOVIMIENTO);
                alert.setSeverity(AlertSeverity.BAJA);
                alert.setTitle("Producto sin movimiento");
                alert.setDescription(String.format("El medicamento %s no ha tenido ventas en los últimos %d días",
                        m.getNombre(), DIAS_SIN_MOVIMIENTO));
                alert.setRelatedEntityId(m.getId());
                alert.setRelatedEntityType("Medicamento");
                alert.setCreatedAt(LocalDateTime.now());
                alertRepository.save(alert);
            }
        }
    }
}