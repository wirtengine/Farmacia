CREATE OR REPLACE VIEW vw_dashboard_resumen AS
SELECT
    (SELECT COUNT(*) FROM ventas WHERE fecha::date = CURRENT_DATE) AS cantidad_ventas_dia,
    (SELECT COALESCE(SUM(total), 0) FROM ventas WHERE fecha::date = CURRENT_DATE) AS total_ventas_dia,
    (SELECT COALESCE(SUM(total), 0) FROM ventas
     WHERE fecha >= date_trunc('month', CURRENT_DATE)::timestamp
       AND fecha <= (date_trunc('month', CURRENT_DATE) + interval '1 month' - interval '1 second')) AS ventas_mes_actual,
    (SELECT COALESCE(SUM(total), 0) FROM ventas
     WHERE fecha >= date_trunc('month', CURRENT_DATE - interval '1 month')::timestamp
       AND fecha <= (date_trunc('month', CURRENT_DATE) - interval '1 second')) AS ventas_mes_anterior;

CREATE OR REPLACE VIEW vw_productos_mas_rentables AS
SELECT
    m.nombre,
    SUM(vd.cantidad * vd.precio_unitario) AS ingresos
FROM venta_detalle vd
         JOIN lote_detalle ld ON vd.lote_detalle_id = ld.id
         JOIN medicamentos m ON ld.medicamento_id = m.id
GROUP BY m.nombre
ORDER BY ingresos DESC;

CREATE OR REPLACE VIEW vw_productos_bajo_stock AS
SELECT
    m.nombre,
    COALESCE(SUM(ld.cantidad), 0)::integer AS stock_total
FROM medicamentos m
         LEFT JOIN lote_detalle ld ON m.id = ld.medicamento_id
         INNER JOIN lotes l ON ld.lote_id = l.id AND l.activo = true
WHERE m.activo = true
GROUP BY m.id, m.nombre
ORDER BY stock_total ASC;

CREATE OR REPLACE VIEW vw_ranking_vendedores AS
SELECT
    u.username,
    COUNT(v.id) AS cantidad_ventas,
    COALESCE(SUM(v.total), 0) AS total_ventas
FROM ventas v
         JOIN usuarios u ON v.usuario_id = u.id
GROUP BY u.username
ORDER BY total_ventas DESC;