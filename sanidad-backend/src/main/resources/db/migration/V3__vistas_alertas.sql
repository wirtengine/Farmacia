
-- Stock actual por medicamento (solo lotes activos)
CREATE OR REPLACE VIEW vw_stock_actual_por_medicamento AS
SELECT
    m.id AS medicamento_id,
    m.nombre AS medicamento_nombre,
    COALESCE(SUM(ld.cantidad), 0)::integer AS stock_actual
FROM medicamentos m
         LEFT JOIN lote_detalle ld ON m.id = ld.medicamento_id
         INNER JOIN lotes l ON ld.lote_id = l.id AND l.activo = true
WHERE m.activo = true
GROUP BY m.id, m.nombre;

-- Ventas por medicamento en los últimos 30 días
CREATE OR REPLACE VIEW vw_ventas_30_dias AS
SELECT
    m.id AS medicamento_id,
    m.nombre AS medicamento_nombre,
    COALESCE(SUM(vd.cantidad), 0)::integer AS total_vendido
FROM medicamentos m
         LEFT JOIN lote_detalle ld ON m.id = ld.medicamento_id
         LEFT JOIN venta_detalle vd ON ld.id = vd.lote_detalle_id
         LEFT JOIN ventas v ON vd.venta_id = v.id
    AND v.fecha >= CURRENT_TIMESTAMP - INTERVAL '30 days'
    AND v.activo = true
WHERE m.activo = true
GROUP BY m.id, m.nombre;

-- Ventas por medicamento en los últimos 90 días
CREATE OR REPLACE VIEW vw_ventas_90_dias AS
SELECT
    m.id AS medicamento_id,
    m.nombre AS medicamento_nombre,
    COALESCE(SUM(vd.cantidad), 0)::integer AS total_vendido
FROM medicamentos m
         LEFT JOIN lote_detalle ld ON m.id = ld.medicamento_id
         LEFT JOIN venta_detalle vd ON ld.id = vd.lote_detalle_id
         LEFT JOIN ventas v ON vd.venta_id = v.id
    AND v.fecha >= CURRENT_TIMESTAMP - INTERVAL '90 days'
    AND v.activo = true
WHERE m.activo = true
GROUP BY m.id, m.nombre;

-- Lotes próximos a vencer (30 días)
CREATE OR REPLACE VIEW vw_lotes_proximos_vencer AS
SELECT
    l.id AS lote_id,
    l.numero_lote,
    l.fecha_vencimiento,
    STRING_AGG(DISTINCT m.nombre, ', ') AS medicamentos,
    SUM(ld.cantidad) AS stock_lote
FROM lotes l
         JOIN lote_detalle ld ON l.id = ld.lote_id
         JOIN medicamentos m ON ld.medicamento_id = m.id
WHERE l.activo = true
  AND l.fecha_vencimiento BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days'
GROUP BY l.id, l.numero_lote, l.fecha_vencimiento
HAVING SUM(ld.cantidad) > 0;

-- Productos sin movimiento en 90 días
CREATE OR REPLACE VIEW vw_productos_sin_movimiento AS
SELECT
    m.id AS medicamento_id,
    m.nombre AS medicamento_nombre
FROM medicamentos m
WHERE m.activo = true
  AND m.id NOT IN (
    SELECT DISTINCT ld.medicamento_id
    FROM venta_detalle vd
             JOIN lote_detalle ld ON vd.lote_detalle_id = ld.id
             JOIN ventas v ON vd.venta_id = v.id AND v.activo = true
    WHERE v.fecha >= CURRENT_TIMESTAMP - INTERVAL '90 days'
    );