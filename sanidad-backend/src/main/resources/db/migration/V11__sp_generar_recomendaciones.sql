CREATE OR REPLACE FUNCTION fn_generar_recomendaciones()
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
v_total INT := 0;
    v_row_count INT;
BEGIN
    -- 1. Resolver recomendaciones viejas (las que ya no aplican)
UPDATE recomendaciones
SET status = 'RESOLVED',
    responded_at = NOW()
WHERE status = 'PENDING'
  AND (
    -- Compra: stock suficiente o sin ventas
    (type = 'PURCHASE_SUGGESTION' AND related_entity_id IN (
        SELECT medicamento_id FROM vw_metricas_productos
        WHERE ventas_30_dias > 0 AND dias_cobertura >= 30
    ))
        OR
        -- Evitar reposición: ya tiene ventas recientes
    (type = 'AVOID_RESTOCK' AND related_entity_id IN (
        SELECT medicamento_id FROM vw_metricas_productos
        WHERE sin_movimiento_90_dias = false
    ))
        OR
        -- Priorizar venta: lote ya no está próximo a vencer
    (type = 'PRIORITIZE_SALE' AND related_entity_id NOT IN (
        SELECT lote_id FROM vw_lotes_proximos_vencer
    ))
    );
GET DIAGNOSTICS v_row_count = ROW_COUNT;
v_total := v_row_count; -- opcional, solo conteo de resueltas

    -- 2. Generar recomendaciones de compra
INSERT INTO recomendaciones (type, priority, title, description, suggested_action, related_entity_id, related_entity_type, created_at, status)
SELECT 'PURCHASE_SUGGESTION',
       CASE WHEN dias_cobertura < 7 THEN 'HIGH' ELSE 'MEDIUM' END,
       'Sugerencia de compra: ' || medicamento_nombre,
       FORMAT('Stock actual: %s unidades. Demanda diaria: %s. Días de cobertura: %s. Se recomienda comprar %s unidades para alcanzar %s días de stock.',
              stock_actual,
              round(ventas_30_dias / 30.0, 1),
              round(dias_cobertura, 1),
              ceil(ventas_30_dias / 30.0 * 30) - stock_actual,
              30),
       'Realizar pedido de ' || (ceil(ventas_30_dias / 30.0 * 30) - stock_actual) || ' unidades al proveedor.',
       medicamento_id,
       'Medicamento',
       NOW(),
       'PENDING'
FROM vw_metricas_productos
WHERE ventas_30_dias > 0
  AND stock_actual > 0
  AND dias_cobertura < 30
  AND medicamento_id NOT IN (
    SELECT related_entity_id FROM recomendaciones
    WHERE type = 'PURCHASE_SUGGESTION' AND status = 'PENDING'
);
GET DIAGNOSTICS v_row_count = ROW_COUNT;
v_total := v_total + v_row_count;

    -- 3. Evitar reposición
INSERT INTO recomendaciones (type, priority, title, description, suggested_action, related_entity_id, related_entity_type, created_at, status)
SELECT 'AVOID_RESTOCK',
       'MEDIUM',
       'Evitar reposición: ' || medicamento_nombre,
       FORMAT('El producto no ha tenido ventas en los últimos 90 días. Se recomienda no realizar nuevos pedidos hasta evaluar su rotación.'),
       'Revisar stock actual y considerar promociones para liquidar existencias.',
       medicamento_id,
       'Medicamento',
       NOW(),
       'PENDING'
FROM vw_metricas_productos
WHERE sin_movimiento_90_dias = true
  AND medicamento_id NOT IN (
    SELECT related_entity_id FROM recomendaciones
    WHERE type = 'AVOID_RESTOCK' AND status = 'PENDING'
);
GET DIAGNOSTICS v_row_count = ROW_COUNT;
v_total := v_total + v_row_count;

    -- 4. Priorizar venta de lotes próximos a vencer
INSERT INTO recomendaciones (type, priority, title, description, suggested_action, related_entity_id, related_entity_type, created_at, status)
SELECT 'PRIORITIZE_SALE',
       'HIGH',
       'Priorizar venta de lote próximo a vencer',
       FORMAT('El lote %s (%s) vence el %s. Se recomienda priorizar su venta o aplicar descuentos para evitar pérdidas.',
              l.numero_lote, l.medicamentos, l.fecha_vencimiento),
       'Aplicar promoción, oferta 2x1 o descuento para liquidar antes del vencimiento.',
       l.lote_id,
       'Lote',
       NOW(),
       'PENDING'
FROM vw_lotes_proximos_vencer l
WHERE l.stock_lote > 0
  AND l.lote_id NOT IN (
    SELECT related_entity_id FROM recomendaciones
    WHERE type = 'PRIORITIZE_SALE' AND status = 'PENDING'
);
GET DIAGNOSTICS v_row_count = ROW_COUNT;
v_total := v_total + v_row_count;

RETURN v_total;
END;
$$;