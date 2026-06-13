CREATE OR REPLACE VIEW vw_productos_vencidos AS
SELECT
    l.id AS lote_id,
    l.numero_lote,
    l.fecha_vencimiento,
    m.id AS medicamento_id,
    m.nombre AS medicamento_nombre,
    ld.cantidad AS cantidad_vencida,
    m.precio_unitario * ld.cantidad AS valor_perdido
FROM lotes l
         JOIN lote_detalle ld ON l.id = ld.lote_id
         JOIN medicamentos m ON ld.medicamento_id = m.id
WHERE l.fecha_vencimiento < CURRENT_DATE
  AND ld.cantidad > 0
  AND l.activo = true;

CREATE OR REPLACE VIEW vw_productos_inmoviles AS
SELECT
    m.id AS medicamento_id,
    m.nombre AS medicamento_nombre,
    COALESCE(SUM(ld.cantidad), 0)::integer AS stock_actual,
    90 AS dias_sin_movimiento,
    m.precio_unitario * COALESCE(SUM(ld.cantidad), 0) AS valor_inmovilizado
FROM medicamentos m
         JOIN lote_detalle ld ON m.id = ld.medicamento_id
         JOIN lotes l ON ld.lote_id = l.id AND l.activo = true
WHERE m.id NOT IN (
    SELECT DISTINCT ld2.medicamento_id
    FROM venta_detalle vd
             JOIN lote_detalle ld2 ON vd.lote_detalle_id = ld2.id
    WHERE vd.venta_id IN (
        SELECT id FROM ventas WHERE fecha >= CURRENT_TIMESTAMP - INTERVAL '90 days'
    )
    )
GROUP BY m.id, m.nombre, m.precio_unitario
HAVING COALESCE(SUM(ld.cantidad), 0) > 0;

CREATE OR REPLACE VIEW vw_inconsistencias_stock AS
SELECT
    ld.id AS lote_detalle_id,
    m.id AS medicamento_id,
    m.nombre AS medicamento_nombre,
    ld.cantidad AS cantidad_lote,
    COALESCE(ul.cantidad_ubicaciones, 0) AS cantidad_ubicaciones,
    ld.cantidad - COALESCE(ul.cantidad_ubicaciones, 0) AS diferencia
FROM lote_detalle ld
         JOIN medicamentos m ON ld.medicamento_id = m.id
         LEFT JOIN (
    SELECT lote_detalle_id, SUM(cantidad) AS cantidad_ubicaciones
    FROM ubicaciones_lote
    WHERE activo = true
    GROUP BY lote_detalle_id
) ul ON ld.id = ul.lote_detalle_id
WHERE ld.cantidad != COALESCE(ul.cantidad_ubicaciones, 0);

CREATE OR REPLACE VIEW vw_resumen_perdidas AS
SELECT
    (SELECT COALESCE(SUM(valor_perdido), 0) FROM vw_productos_vencidos) AS total_perdidas_vencimiento,
    (SELECT COUNT(*) FROM vw_productos_vencidos) AS cantidad_productos_vencidos,
    (SELECT COALESCE(SUM(valor_inmovilizado), 0) FROM vw_productos_inmoviles) AS total_inmovilizado,
    (SELECT COUNT(*) FROM vw_productos_inmoviles) AS cantidad_productos_inmoviles,
    (SELECT COUNT(*) FROM vw_inconsistencias_stock) AS cantidad_inconsistencias;