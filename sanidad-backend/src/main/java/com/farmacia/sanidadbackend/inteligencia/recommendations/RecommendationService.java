package com.farmacia.sanidadbackend.inteligencia.recommendations;

import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Autowired
    private LoteDetalleRepository loteDetalleRepository;

    @Autowired
    private VentaDetalleRepository ventaDetalleRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    // Parámetros ajustables
    private static final int DIAS_VENTAS_PROMEDIO = 30;
    private static final int DIAS_COBERTURA_DESEADA = 30;
    private static final int DIAS_SIN_VENTAS_BAJA_ROTACION = 90;
    private static final int DIAS_VENCIMIENTO_PRIORIDAD = 30;

    @Transactional
    @Scheduled(cron = "0 30 1 * * ?") // Cada día a las 1:30 AM
    public void generarRecomendaciones() {
        logger.info("Iniciando generación automática de recomendaciones...");
        resolverRecomendacionesViejas();
        generarRecomendacionesCompra();
        generarRecomendacionesEvitarReposicion();
        generarRecomendacionesPriorizarVenta();
        logger.info("Finalizada generación automática de recomendaciones.");
    }

    /**
     * Marca como RESOLVED las recomendaciones pendientes que ya no cumplen las condiciones.
     */
    private void resolverRecomendacionesViejas() {
        List<Recommendation> pending = recommendationRepository.findByStatusOrderByPriorityDescCreatedAtDesc(RecommendationStatus.PENDING);
        if (pending.isEmpty()) return;

        // Datos necesarios para evaluar cada tipo
        LocalDate hoy = LocalDate.now();
        LocalDateTime hasta = LocalDateTime.now();
        LocalDateTime desdeVentas = hasta.minusDays(DIAS_VENTAS_PROMEDIO);
        LocalDateTime limiteSinMovimiento = hasta.minusDays(DIAS_SIN_VENTAS_BAJA_ROTACION);

        // Mapa de stock actual por medicamento
        List<Object[]> stockResult = loteDetalleRepository.findStockActualPorMedicamento(hoy);
        Map<Long, Integer> stockActualMap = stockResult.stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).intValue()));

        // Mapa de ventas totales (últimos 30 días)
        List<Object[]> ventasTotales = ventaDetalleRepository.sumCantidadByMedicamentoEntreFechas(desdeVentas, hasta);
        Map<Long, Integer> ventasTotalesMap = ventasTotales.stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).intValue()));

        // Mapa de ventas recientes (para evitar reposición)
        List<Object[]> ventasRecientes = ventaDetalleRepository.sumCantidadByMedicamentoEntreFechas(limiteSinMovimiento, hasta);
        Set<Long> medicamentosConVentasRecientes = ventasRecientes.stream()
                .map(r -> ((Number) r[0]).longValue())
                .collect(Collectors.toSet());

        for (Recommendation rec : pending) {
            boolean shouldResolve = false;

            switch (rec.getType()) {
                case PURCHASE_SUGGESTION:
                    Long medId = rec.getRelatedEntityId();
                    int stock = stockActualMap.getOrDefault(medId, 0);
                    int ventas = ventasTotalesMap.getOrDefault(medId, 0);
                    if (ventas > 0) {
                        double promedio = ventas / (double) DIAS_VENTAS_PROMEDIO;
                        int cobertura = (int) (stock / promedio);
                        if (cobertura >= DIAS_COBERTURA_DESEADA) {
                            shouldResolve = true;
                        }
                    } else {
                        // Sin ventas, la recomendación de compra ya no tiene sentido
                        shouldResolve = true;
                    }
                    break;

                case AVOID_RESTOCK:
                    Long prodId = rec.getRelatedEntityId();
                    if (medicamentosConVentasRecientes.contains(prodId)) {
                        // Ya tuvo ventas, se puede reponer
                        shouldResolve = true;
                    }
                    break;

                case PRIORITIZE_SALE:
                    Long loteId = rec.getRelatedEntityId();
                    Optional<Lote> optLote = loteRepository.findById(loteId);
                    if (optLote.isEmpty() || !optLote.get().isActivo()) {
                        shouldResolve = true;
                        break;
                    }
                    Lote lote = optLote.get();
                    // Verificar si aún está próximo a vencer
                    LocalDate vencimiento = lote.getFechaVencimiento();
                    if (vencimiento.isBefore(hoy) || vencimiento.isAfter(hoy.plusDays(DIAS_VENCIMIENTO_PRIORIDAD))) {
                        shouldResolve = true;
                        break;
                    }
                    // Verificar si aún tiene stock (sumar cantidad de sus detalles)
                    int stockLote = lote.getDetalles().stream().mapToInt(LoteDetalle::getCantidad).sum();
                    if (stockLote <= 0) {
                        shouldResolve = true;
                    }
                    break;
            }

            if (shouldResolve) {
                rec.setStatus(RecommendationStatus.RESOLVED);
                rec.setRespondedAt(LocalDateTime.now());
                recommendationRepository.save(rec);
                logger.info("Recomendación {} resuelta automáticamente.", rec.getId());
            }
        }
    }

    /**
     * Sugiere comprar productos con alta demanda y bajo stock.
     */
    private void generarRecomendacionesCompra() {
        LocalDate hoy = LocalDate.now();
        List<Object[]> stockResult = loteDetalleRepository.findStockActualPorMedicamento(hoy);
        Map<Long, Integer> stockActualMap = stockResult.stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).intValue()));

        LocalDateTime hasta = LocalDateTime.now();
        LocalDateTime desde = hasta.minusDays(DIAS_VENTAS_PROMEDIO);
        List<Object[]> ventasResult = ventaDetalleRepository.sumCantidadByMedicamentoEntreFechas(desde, hasta);
        Map<Long, Integer> ventasTotalesMap = ventasResult.stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).intValue()));

        List<Medicamento> medicamentos = medicamentoRepository.findByActivoTrue();

        for (Medicamento m : medicamentos) {
            int stock = stockActualMap.getOrDefault(m.getId(), 0);
            int ventas = ventasTotalesMap.getOrDefault(m.getId(), 0);
            if (ventas == 0) continue;

            double promedio = ventas / (double) DIAS_VENTAS_PROMEDIO;
            int cobertura = (int) (stock / promedio);
            if (cobertura < DIAS_COBERTURA_DESEADA && stock > 0) {
                // Evitar duplicados pendientes
                if (!recommendationRepository.existsByTypeAndRelatedEntityIdAndStatus(
                        RecommendationType.PURCHASE_SUGGESTION, m.getId(), RecommendationStatus.PENDING)) {
                    int cantidadSugerida = (int) Math.ceil(promedio * DIAS_COBERTURA_DESEADA) - stock;
                    Recommendation rec = new Recommendation();
                    rec.setType(RecommendationType.PURCHASE_SUGGESTION);
                    rec.setPriority(cobertura < 7 ? RecommendationPriority.HIGH : RecommendationPriority.MEDIUM);
                    rec.setTitle("Sugerencia de compra: " + m.getNombre());
                    rec.setDescription(String.format(
                            "Stock actual: %d unidades. Demanda diaria: %.1f. Días de cobertura: %d. Se recomienda comprar %d unidades para alcanzar %d días de stock.",
                            stock, promedio, cobertura, cantidadSugerida, DIAS_COBERTURA_DESEADA));
                    rec.setSuggestedAction("Realizar pedido de " + cantidadSugerida + " unidades al proveedor.");
                    rec.setRelatedEntityId(m.getId());
                    rec.setRelatedEntityType("Medicamento");
                    rec.setCreatedAt(LocalDateTime.now());
                    recommendationRepository.save(rec);
                    logger.info("Generada recomendación de compra para {}", m.getNombre());
                }
            }
        }
    }

    /**
     * Evita reponer productos con baja rotación (sin ventas en los últimos N días).
     */
    private void generarRecomendacionesEvitarReposicion() {
        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_SIN_VENTAS_BAJA_ROTACION);
        List<Object[]> ventasRecientes = ventaDetalleRepository.sumCantidadByMedicamentoEntreFechas(limite, LocalDateTime.now());
        Set<Long> medicamentosConVentas = ventasRecientes.stream()
                .map(r -> ((Number) r[0]).longValue())
                .collect(Collectors.toSet());

        List<Medicamento> medicamentos = medicamentoRepository.findByActivoTrue();

        for (Medicamento m : medicamentos) {
            if (!medicamentosConVentas.contains(m.getId())) {
                if (!recommendationRepository.existsByTypeAndRelatedEntityIdAndStatus(
                        RecommendationType.AVOID_RESTOCK, m.getId(), RecommendationStatus.PENDING)) {
                    Recommendation rec = new Recommendation();
                    rec.setType(RecommendationType.AVOID_RESTOCK);
                    rec.setPriority(RecommendationPriority.MEDIUM);
                    rec.setTitle("Evitar reposición: " + m.getNombre());
                    rec.setDescription(String.format(
                            "El producto no ha tenido ventas en los últimos %d días. Se recomienda no realizar nuevos pedidos hasta evaluar su rotación.",
                            DIAS_SIN_VENTAS_BAJA_ROTACION));
                    rec.setSuggestedAction("Revisar stock actual y considerar promociones para liquidar existencias.");
                    rec.setRelatedEntityId(m.getId());
                    rec.setRelatedEntityType("Medicamento");
                    rec.setCreatedAt(LocalDateTime.now());
                    recommendationRepository.save(rec);
                    logger.info("Generada recomendación de evitar reposición para {}", m.getNombre());
                }
            }
        }
    }

    /**
     * Priorizar la venta de lotes próximos a vencer.
     */
    private void generarRecomendacionesPriorizarVenta() {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(DIAS_VENCIMIENTO_PRIORIDAD);
        List<Lote> lotes = loteRepository.findByFechaVencimientoBetween(hoy, limite);

        for (Lote lote : lotes) {
            // Verificar si aún tiene stock (suma de cantidades de sus detalles)
            int stockLote = lote.getDetalles().stream().mapToInt(LoteDetalle::getCantidad).sum();
            if (stockLote <= 0) continue; // No hay stock que vender

            if (!recommendationRepository.existsByTypeAndRelatedEntityIdAndStatus(
                    RecommendationType.PRIORITIZE_SALE, lote.getId(), RecommendationStatus.PENDING)) {
                String medicamentosNombres = lote.getDetalles().stream()
                        .map(ld -> ld.getMedicamento().getNombre())
                        .distinct()
                        .collect(Collectors.joining(", "));
                Recommendation rec = new Recommendation();
                rec.setType(RecommendationType.PRIORITIZE_SALE);
                rec.setPriority(RecommendationPriority.HIGH);
                rec.setTitle("Priorizar venta de lote próximo a vencer");
                rec.setDescription(String.format(
                        "El lote %s (medicamentos: %s) vence el %s. Se recomienda priorizar su venta o aplicar descuentos para evitar pérdidas.",
                        lote.getNumeroLote(), medicamentosNombres, lote.getFechaVencimiento()));
                rec.setSuggestedAction("Aplicar promoción, oferta 2x1 o descuento para liquidar antes del vencimiento.");
                rec.setRelatedEntityId(lote.getId());
                rec.setRelatedEntityType("Lote");
                rec.setCreatedAt(LocalDateTime.now());
                recommendationRepository.save(rec);
                logger.info("Generada recomendación de priorizar venta para lote {}", lote.getNumeroLote());
            }
        }
    }

    /**
     * Obtiene todas las recomendaciones pendientes (para el asistente u otras consultas).
     */
    public List<Recommendation> obtenerRecomendacionesPendientes() {
        return recommendationRepository.findByStatusOrderByPriorityDescCreatedAtDesc(RecommendationStatus.PENDING);
    }
}