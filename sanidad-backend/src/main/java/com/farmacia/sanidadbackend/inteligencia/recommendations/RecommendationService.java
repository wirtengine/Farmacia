package com.farmacia.sanidadbackend.inteligencia.recommendations;

import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecommendationRepository recommendationRepository;

    private static final int DIAS_COBERTURA_DESEADA = 30;
    private static final int DIAS_SIN_VENTAS = 90;
    private static final int DIAS_VENCIMIENTO = 30;

    /**
     * Programa diario (1:30 AM) que invoca la función almacenada fn_generar_recomendaciones
     * para generar y resolver recomendaciones automáticamente en la base de datos.
     */
    @Transactional
    @Scheduled(cron = "0 30 1 * * ?")
    public void generarRecomendaciones() {
        String sql = "SELECT fn_generar_recomendaciones()";
        Integer generadas = jdbcTemplate.queryForObject(sql, Integer.class);
        logger.info("Recomendaciones generadas/resueltas: {}", generadas);
    }

    /**
     * Devuelve las recomendaciones pendientes (usado por AssistantService).
     */
    public List<Recommendation> obtenerRecomendacionesPendientes() {
        return recommendationRepository.findByStatusOrderByPriorityDescCreatedAtDesc(RecommendationStatus.PENDING);
    }

    // Los siguientes métodos privados se conservan por si se requiere ejecutar verificaciones
    // individuales de forma manual o desde otras partes de la aplicación.
    // No son invocados por el método programado actual.

    private void resolverRecomendacionesViejas() {
        List<Recommendation> pending = recommendationRepository.findByStatusOrderByPriorityDescCreatedAtDesc(RecommendationStatus.PENDING);
        if (pending.isEmpty()) return;

        List<MetricaRec> metricas = jdbcTemplate.query(
                "SELECT medicamento_id, dias_cobertura, sin_movimiento_90_dias FROM vw_metricas_productos",
                (rs, rn) -> new MetricaRec(rs.getLong(1), rs.getDouble(2), rs.getBoolean(3))
        );
        var lotesIds = jdbcTemplate.queryForList("SELECT lote_id FROM vw_lotes_proximos_vencer", Long.class).stream().collect(Collectors.toSet());

        for (Recommendation r : pending) {
            boolean resolver = false;
            switch (r.getType()) {
                case PURCHASE_SUGGESTION -> {
                    MetricaRec m = metricas.stream().filter(x -> x.id.equals(r.getRelatedEntityId())).findFirst().orElse(null);
                    if (m == null || m.cobertura >= DIAS_COBERTURA_DESEADA) resolver = true;
                }
                case AVOID_RESTOCK -> {
                    MetricaRec m = metricas.stream().filter(x -> x.id.equals(r.getRelatedEntityId())).findFirst().orElse(null);
                    if (m == null || !m.sinMovimiento) resolver = true;
                }
                case PRIORITIZE_SALE -> {
                    if (!lotesIds.contains(r.getRelatedEntityId())) resolver = true;
                }
            }
            if (resolver) {
                r.setStatus(RecommendationStatus.RESOLVED);
                r.setRespondedAt(LocalDateTime.now());
                recommendationRepository.save(r);
            }
        }
    }

    private void generarRecomendacionesCompra() {
        jdbcTemplate.query(
                "SELECT medicamento_id, medicamento_nombre, stock_actual, ventas_30_dias, dias_cobertura FROM vw_metricas_productos WHERE ventas_30_dias > 0 AND stock_actual > 0",
                (rs) -> {
                    long id = rs.getLong("medicamento_id");
                    String nombre = rs.getString("medicamento_nombre");
                    int stock = rs.getInt("stock_actual");
                    int ventas = rs.getInt("ventas_30_dias");
                    double cobertura = rs.getDouble("dias_cobertura");
                    if (cobertura < DIAS_COBERTURA_DESEADA && !recommendationRepository.existsByTypeAndRelatedEntityIdAndStatus(RecommendationType.PURCHASE_SUGGESTION, id, RecommendationStatus.PENDING)) {
                        double prom = ventas / 30.0;
                        int sugerida = (int) Math.ceil(prom * DIAS_COBERTURA_DESEADA) - stock;
                        Recommendation rec = new Recommendation();
                        rec.setType(RecommendationType.PURCHASE_SUGGESTION);
                        rec.setPriority(cobertura < 7 ? RecommendationPriority.HIGH : RecommendationPriority.MEDIUM);
                        rec.setTitle("Sugerencia de compra: " + nombre);
                        rec.setDescription(String.format("Stock %d, demanda diaria %.1f, cobertura %.0f días. Comprar %d unidades.", stock, prom, cobertura, sugerida));
                        rec.setSuggestedAction("Pedir " + sugerida + " unidades al proveedor.");
                        rec.setRelatedEntityId(id);
                        rec.setRelatedEntityType("Medicamento");
                        rec.setCreatedAt(LocalDateTime.now());
                        recommendationRepository.save(rec);
                    }
                }
        );
    }

    private void generarRecomendacionesEvitarReposicion() {
        jdbcTemplate.query(
                "SELECT medicamento_id, medicamento_nombre FROM vw_metricas_productos WHERE sin_movimiento_90_dias = true",
                (rs) -> {
                    long id = rs.getLong("medicamento_id");
                    if (!recommendationRepository.existsByTypeAndRelatedEntityIdAndStatus(RecommendationType.AVOID_RESTOCK, id, RecommendationStatus.PENDING)) {
                        Recommendation rec = new Recommendation();
                        rec.setType(RecommendationType.AVOID_RESTOCK);
                        rec.setPriority(RecommendationPriority.MEDIUM);
                        rec.setTitle("Evitar reposición: " + rs.getString("medicamento_nombre"));
                        rec.setDescription("Sin ventas en " + DIAS_SIN_VENTAS + " días. No reponer.");
                        rec.setSuggestedAction("Revisar stock y considerar promociones.");
                        rec.setRelatedEntityId(id);
                        rec.setRelatedEntityType("Medicamento");
                        rec.setCreatedAt(LocalDateTime.now());
                        recommendationRepository.save(rec);
                    }
                }
        );
    }

    private void generarRecomendacionesPriorizarVenta() {
        jdbcTemplate.query(
                "SELECT lote_id, numero_lote, fecha_vencimiento, medicamentos, stock_lote FROM vw_lotes_proximos_vencer",
                (rs) -> {
                    long id = rs.getLong("lote_id");
                    if (!recommendationRepository.existsByTypeAndRelatedEntityIdAndStatus(RecommendationType.PRIORITIZE_SALE, id, RecommendationStatus.PENDING)) {
                        Recommendation rec = new Recommendation();
                        rec.setType(RecommendationType.PRIORITIZE_SALE);
                        rec.setPriority(RecommendationPriority.HIGH);
                        rec.setTitle("Priorizar venta lote próximo a vencer");
                        rec.setDescription(String.format("Lote %s (%s) vence %s.", rs.getString("numero_lote"), rs.getString("medicamentos"), rs.getString("fecha_vencimiento")));
                        rec.setSuggestedAction("Aplicar descuento o promoción.");
                        rec.setRelatedEntityId(id);
                        rec.setRelatedEntityType("Lote");
                        rec.setCreatedAt(LocalDateTime.now());
                        recommendationRepository.save(rec);
                    }
                }
        );
    }

    private record MetricaRec(Long id, double cobertura, boolean sinMovimiento) {}
}