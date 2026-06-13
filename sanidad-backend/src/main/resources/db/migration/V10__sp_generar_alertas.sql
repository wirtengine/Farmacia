CREATE OR REPLACE FUNCTION fn_generar_alertas()
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
v_alertas INT := 0;
    v_row_count INT;
BEGIN
    -- 1. Stock crítico
INSERT INTO alertas (type, severity, title, description, related_entity_id, related_entity_type, created_at, status)
SELECT 'STOCK_CRITICO',
       CASE WHEN m.stock_actual < m.umbral / 2 THEN 'ALTA' ELSE 'MEDIA' END,
       'Stock crítico: ' || m.nombre,
       FORMAT('Stock actual: %s, venta estimada 7 días: %s', m.stock_actual, m.umbral),
       m.medicamento_id,
       'Medicamento',
       NOW(),
       'PENDING'
FROM (
         SELECT
             s.medicamento_id,
             s.medicamento_nombre AS nombre,
             s.stock_actual,
             ceil(v.total_vendido / 30.0 * 7) AS umbral
         FROM vw_stock_actual_por_medicamento s
                  JOIN vw_ventas_30_dias v ON s.medicamento_id = v.medicamento_id
         WHERE v.total_vendido > 0
           AND s.stock_actual < ceil(v.total_vendido / 30.0 * 7)
     ) m
WHERE NOT EXISTS (
    SELECT 1 FROM alertas
    WHERE type = 'STOCK_CRITICO'
      AND related_entity_id = m.medicamento_id
      AND status = 'PENDING'
);
GET DIAGNOSTICS v_row_count = ROW_COUNT;
v_alertas := v_alertas + v_row_count;

    -- 2. Lotes próximos a vencer
INSERT INTO alertas (type, severity, title, description, related_entity_id, related_entity_type, created_at, status)
SELECT 'LOTE_PROXIMO_VENCER',
       'MEDIA',
       'Lote próximo a vencer',
       FORMAT('Lote %s (%s) vence el %s', l.numero_lote, l.medicamentos, l.fecha_vencimiento),
       l.lote_id,
       'Lote',
       NOW(),
       'PENDING'
FROM vw_lotes_proximos_vencer l
WHERE NOT EXISTS (
    SELECT 1 FROM alertas
    WHERE type = 'LOTE_PROXIMO_VENCER'
      AND related_entity_id = l.lote_id
      AND status = 'PENDING'
);
GET DIAGNOSTICS v_row_count = ROW_COUNT;
v_alertas := v_alertas + v_row_count;

    -- 3. Productos sin movimiento
INSERT INTO alertas (type, severity, title, description, related_entity_id, related_entity_type, created_at, status)
SELECT 'PRODUCTO_SIN_MOVIMIENTO',
       'BAJA',
       'Producto sin movimiento',
       FORMAT('El medicamento %s no ha tenido ventas en los últimos 90 días', s.medicamento_nombre),
       s.medicamento_id,
       'Medicamento',
       NOW(),
       'PENDING'
FROM vw_productos_sin_movimiento s
WHERE NOT EXISTS (
    SELECT 1 FROM alertas
    WHERE type = 'PRODUCTO_SIN_MOVIMIENTO'
      AND related_entity_id = s.medicamento_id
      AND status = 'PENDING'
);
GET DIAGNOSTICS v_row_count = ROW_COUNT;
v_alertas := v_alertas + v_row_count;

RETURN v_alertas;
END;
$$;