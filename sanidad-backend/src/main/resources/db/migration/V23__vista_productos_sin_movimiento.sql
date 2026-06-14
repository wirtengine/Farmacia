DROP VIEW IF EXISTS vw_productos_sin_movimiento;

CREATE OR REPLACE VIEW vw_productos_sin_movimiento AS
SELECT
    m.id AS medicamento_id,
    m.nombre AS nombre,
    COALESCE(SUM(ld.cantidad), 0)::integer AS stock_actual,
    MAX(v.fecha) AS ultima_fecha_venta,
    COALESCE(SUM(vd.cantidad), 0)::integer AS unidades_vendidas
FROM medicamentos m
         LEFT JOIN lote_detalle ld
                   ON ld.medicamento_id = m.id
         LEFT JOIN venta_detalle vd
                   ON vd.lote_detalle_id = ld.id
         LEFT JOIN ventas v
                   ON v.id = vd.venta_id
WHERE m.activo = true
GROUP BY m.id, m.nombre
HAVING COALESCE(SUM(ld.cantidad), 0) > 0
   AND (
    MAX(v.fecha) IS NULL
        OR MAX(v.fecha) < CURRENT_DATE - INTERVAL '15 days'
    )
ORDER BY ultima_fecha_venta NULLS FIRST, stock_actual DESC;