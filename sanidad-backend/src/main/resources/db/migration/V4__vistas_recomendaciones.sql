CREATE OR REPLACE VIEW vw_metricas_productos AS
SELECT
    s.medicamento_id,
    s.medicamento_nombre,
    s.stock_actual,
    COALESCE(v30.total_vendido, 0) AS ventas_30_dias,
    COALESCE(v90.total_vendido, 0) AS ventas_90_dias,
    CASE
        WHEN COALESCE(v30.total_vendido, 0) > 0
            THEN s.stock_actual / (v30.total_vendido / 30.0)
        ELSE 999999
        END AS dias_cobertura,
    CASE
        WHEN COALESCE(v90.total_vendido, 0) = 0 THEN true
        ELSE false
        END AS sin_movimiento_90_dias
FROM vw_stock_actual_por_medicamento s
         LEFT JOIN vw_ventas_30_dias v30 ON s.medicamento_id = v30.medicamento_id
         LEFT JOIN vw_ventas_90_dias v90 ON s.medicamento_id = v90.medicamento_id;