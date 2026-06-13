package com.farmacia.sanidadbackend.inteligencia.alerts;

import com.farmacia.sanidadbackend.model.*;
import com.farmacia.sanidadbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlertService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AlertRepository alertRepository;

    private static final int DIAS_STOCK_CRITICO = 7;
    private static final int DIAS_LOTE_VENCE = 30;
    private static final int DIAS_SIN_MOVIMIENTO = 90;

    @Transactional
    @Scheduled(cron = "0 0 1 * * ?")
    public void generarAlertas() {
        resolverAlertasViejas();
        verificarStockCritico();
        verificarLotesProximosVencer();
        verificarProductosSinMovimiento();
    }

    private void resolverAlertasViejas() {
        List<Alert> pending = alertRepository.findByStatusOrderByCreatedAtDesc(AlertStatus.PENDING);
        if (pending.isEmpty()) return;

        Set<Long> lotesProximosIds = jdbcTemplate.queryForList(
                "SELECT lote_id FROM vw_lotes_proximos_vencer", Long.class
        ).stream().collect(Collectors.toSet());


        List<MetricaAlert> metricas = jdbcTemplate.query(
                "SELECT s.medicamento_id, s.stock_actual, COALESCE(v.total_vendido,0) AS ventas_30_dias " +
                        "FROM vw_stock_actual_por_medicamento s " +
                        "LEFT JOIN vw_ventas_30_dias v ON s.medicamento_id = v.medicamento_id",
                (rs, rn) -> new MetricaAlert(rs.getLong(1), rs.getInt(2), rs.getInt(3))
        );

        Set<Long> sinMovimiento = jdbcTemplate.queryForList(
                "SELECT medicamento_id FROM vw_productos_sin_movimiento", Long.class
        ).stream().collect(Collectors.toSet());

        for (Alert a : pending) {
            boolean resolver = false;
            switch (a.getType()) {
                case STOCK_CRITICO -> {
                    MetricaAlert m = metricas.stream().filter(x -> x.id.equals(a.getRelatedEntityId())).findFirst().orElse(null);
                    if (m == null || m.ventas30 == 0) resolver = true;
                    else {
                        double prom = m.ventas30 / 30.0;
                        int umbral = (int) Math.ceil(prom * DIAS_STOCK_CRITICO);
                        if (m.stock >= umbral) resolver = true;
                    }
                }
                case LOTE_PROXIMO_VENCER -> {
                    if (!lotesProximosIds.contains(a.getRelatedEntityId())) resolver = true;
                }
                case PRODUCTO_SIN_MOVIMIENTO -> {
                    if (!sinMovimiento.contains(a.getRelatedEntityId())) resolver = true;
                }
            }
            if (resolver) {
                a.setStatus(AlertStatus.RESOLVED);
                a.setAcknowledgedAt(LocalDateTime.now());
                alertRepository.save(a);
            }
        }
    }

    private void verificarStockCritico() {

        jdbcTemplate.query(
                "SELECT s.medicamento_id, s.medicamento_nombre, s.stock_actual, COALESCE(v.total_vendido,0) AS ventas_30_dias " +
                        "FROM vw_stock_actual_por_medicamento s " +
                        "LEFT JOIN vw_ventas_30_dias v ON s.medicamento_id = v.medicamento_id " +
                        "WHERE COALESCE(v.total_vendido,0) > 0 AND s.stock_actual > 0",
                (rs) -> {
                    long id = rs.getLong("medicamento_id");
                    String nombre = rs.getString("medicamento_nombre");
                    int stock = rs.getInt("stock_actual");
                    int ventas = rs.getInt("ventas_30_dias");
                    double prom = ventas / 30.0;
                    int umbral = (int) Math.ceil(prom * DIAS_STOCK_CRITICO);
                    if (stock < umbral && !alertRepository.existsByTypeAndRelatedEntityIdAndStatus(AlertType.STOCK_CRITICO, id, AlertStatus.PENDING)) {
                        Alert a = new Alert();
                        a.setType(AlertType.STOCK_CRITICO);
                        a.setSeverity(stock < (umbral/2) ? AlertSeverity.ALTA : AlertSeverity.MEDIA);
                        a.setTitle("Stock crítico: " + nombre);
                        a.setDescription(String.format("Stock actual: %d, venta estimada %d días: %d", stock, DIAS_STOCK_CRITICO, umbral));
                        a.setRelatedEntityId(id);
                        a.setRelatedEntityType("Medicamento");
                        a.setCreatedAt(LocalDateTime.now());
                        alertRepository.save(a);
                    }
                }
        );
    }

    private void verificarLotesProximosVencer() {
        jdbcTemplate.query(
                "SELECT lote_id, numero_lote, fecha_vencimiento, medicamentos, stock_lote FROM vw_lotes_proximos_vencer",
                (rs) -> {
                    long id = rs.getLong("lote_id");
                    if (!alertRepository.existsByTypeAndRelatedEntityIdAndStatus(AlertType.LOTE_PROXIMO_VENCER, id, AlertStatus.PENDING)) {
                        Alert a = new Alert();
                        a.setType(AlertType.LOTE_PROXIMO_VENCER);
                        a.setSeverity(AlertSeverity.MEDIA);
                        a.setTitle("Lote próximo a vencer");
                        a.setDescription(String.format("Lote %s (%s) vence el %s", rs.getString("numero_lote"), rs.getString("medicamentos"), rs.getString("fecha_vencimiento")));
                        a.setRelatedEntityId(id);
                        a.setRelatedEntityType("Lote");
                        a.setCreatedAt(LocalDateTime.now());
                        alertRepository.save(a);
                    }
                }
        );
    }

    private void verificarProductosSinMovimiento() {
        jdbcTemplate.query(
                "SELECT medicamento_id, medicamento_nombre FROM vw_productos_sin_movimiento",
                (rs) -> {
                    long id = rs.getLong("medicamento_id");
                    if (!alertRepository.existsByTypeAndRelatedEntityIdAndStatus(AlertType.PRODUCTO_SIN_MOVIMIENTO, id, AlertStatus.PENDING)) {
                        Alert a = new Alert();
                        a.setType(AlertType.PRODUCTO_SIN_MOVIMIENTO);
                        a.setSeverity(AlertSeverity.BAJA);
                        a.setTitle("Producto sin movimiento");
                        a.setDescription(String.format("El medicamento %s no ha tenido ventas en los últimos %d días", rs.getString("medicamento_nombre"), DIAS_SIN_MOVIMIENTO));
                        a.setRelatedEntityId(id);
                        a.setRelatedEntityType("Medicamento");
                        a.setCreatedAt(LocalDateTime.now());
                        alertRepository.save(a);
                    }
                }
        );
    }

    private record MetricaAlert(Long id, int stock, int ventas30) {}
}