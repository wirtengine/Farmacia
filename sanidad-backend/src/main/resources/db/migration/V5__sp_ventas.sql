CREATE OR REPLACE FUNCTION fn_crear_venta(
    p_cliente_id BIGINT,
    p_usuario_id BIGINT,
    p_detalles JSONB,
    p_receta_id BIGINT DEFAULT NULL,
    p_monto_efectivo NUMERIC DEFAULT 0,
    p_monto_saldo NUMERIC DEFAULT 0
)
RETURNS TABLE(venta_id BIGINT, numero_factura VARCHAR)
LANGUAGE plpgsql
AS $$
DECLARE
v_subtotal NUMERIC(12,2) := 0;
    v_iva NUMERIC(12,2) := 0;
    v_total NUMERIC(12,2) := 0;
    v_cambio NUMERIC(12,2) := 0;
    v_total_pagado NUMERIC(12,2) := 0;
    v_cliente_id BIGINT := p_cliente_id;
    v_numero_factura VARCHAR;
    v_venta_id BIGINT;
    detalle RECORD;
    v_precio_unitario NUMERIC;
    v_subtotal_detalle NUMERIC(12,2);
    v_cantidad_restante INT;
    v_stock_lote INT;
    v_ubicacion RECORD;
    v_necesita_receta BOOLEAN := FALSE;
    v_medicamento_receta BOOLEAN;
BEGIN
    -- Verificar usuario
    PERFORM id FROM usuarios WHERE id = p_usuario_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario no encontrado';
END IF;

    -- Verificar cliente
    IF v_cliente_id IS NOT NULL THEN
        PERFORM id FROM clientes WHERE id = v_cliente_id AND activo = true;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Cliente no encontrado o inactivo';
END IF;
END IF;

    -- Verificar necesidad de receta
FOR detalle IN SELECT * FROM jsonb_to_recordset(p_detalles) AS x("loteDetalleId" BIGINT, "cantidad" INT)
    LOOP
SELECT m.receta INTO v_medicamento_receta
FROM lote_detalle ld
         JOIN medicamentos m ON ld.medicamento_id = m.id
WHERE ld.id = detalle."loteDetalleId";
IF v_medicamento_receta = true THEN
            v_necesita_receta := TRUE;
END IF;
END LOOP;

    IF v_necesita_receta THEN
        IF p_receta_id IS NULL THEN
            RAISE EXCEPTION 'Esta venta incluye medicamentos que requieren receta. Debe proporcionar un ID de receta válida.';
END IF;
END IF;

    -- Generar número de factura
    v_numero_factura := 'F' || to_char(CURRENT_DATE, 'YYYYMMDD') || '-' || lpad(floor(random()*9000+1000)::text, 4, '0');

    -- Insertar venta
INSERT INTO ventas (numero_factura, fecha, cliente_id, usuario_id, subtotal, iva, total, tipo, activo)
VALUES (v_numero_factura, NOW(), v_cliente_id, p_usuario_id, 0, 0, 0,
        CASE WHEN v_cliente_id IS NOT NULL THEN 'CLIENTE' ELSE 'RAPIDA' END, true)
    RETURNING id INTO v_venta_id;

-- Procesar detalles
FOR detalle IN SELECT * FROM jsonb_to_recordset(p_detalles) AS x("loteDetalleId" BIGINT, "cantidad" INT)
    LOOP
SELECT m.precio_unitario INTO v_precio_unitario
FROM lote_detalle ld JOIN medicamentos m ON ld.medicamento_id = m.id
WHERE ld.id = detalle."loteDetalleId";

SELECT cantidad INTO v_stock_lote FROM lote_detalle WHERE id = detalle."loteDetalleId" FOR UPDATE;
IF v_stock_lote < detalle."cantidad" THEN
            RAISE EXCEPTION 'Stock insuficiente para lote_detalle %', detalle."loteDetalleId";
END IF;

        v_cantidad_restante := detalle."cantidad";
FOR v_ubicacion IN SELECT * FROM ubicaciones_lote WHERE lote_detalle_id = detalle."loteDetalleId" AND activo = true ORDER BY id
    LOOP
            EXIT WHEN v_cantidad_restante <= 0;
IF v_ubicacion.cantidad >= v_cantidad_restante THEN
UPDATE ubicaciones_lote SET cantidad = cantidad - v_cantidad_restante,
                            activo = CASE WHEN cantidad - v_cantidad_restante = 0 THEN false ELSE true END
WHERE id = v_ubicacion.id;
v_cantidad_restante := 0;
ELSE
                v_cantidad_restante := v_cantidad_restante - v_ubicacion.cantidad;
UPDATE ubicaciones_lote SET cantidad = 0, activo = false WHERE id = v_ubicacion.id;
END IF;
END LOOP;

UPDATE lote_detalle SET cantidad = cantidad - detalle."cantidad" WHERE id = detalle."loteDetalleId";

v_subtotal_detalle := v_precio_unitario * detalle."cantidad";
INSERT INTO venta_detalle (venta_id, lote_detalle_id, cantidad, precio_unitario, subtotal)
VALUES (v_venta_id, detalle."loteDetalleId", detalle."cantidad", v_precio_unitario, v_subtotal_detalle);

v_subtotal := v_subtotal + v_subtotal_detalle;
END LOOP;

    -- Totales
    v_iva := round(v_subtotal * 0.15, 2);
    v_total := v_subtotal + v_iva;
    v_total_pagado := COALESCE(p_monto_efectivo, 0) + COALESCE(p_monto_saldo, 0);
    v_cambio := v_total_pagado - v_total;
    IF v_cambio < 0 THEN
        RAISE EXCEPTION 'El pago es insuficiente';
END IF;

UPDATE ventas SET subtotal = v_subtotal, iva = v_iva, total = v_total,
                  monto_efectivo = COALESCE(p_monto_efectivo, 0), monto_usado_saldo = COALESCE(p_monto_saldo, 0), cambio = v_cambio
WHERE id = v_venta_id;

IF v_cliente_id IS NOT NULL AND COALESCE(p_monto_saldo, 0) > 0 THEN
UPDATE clientes SET saldo = saldo - p_monto_saldo WHERE id = v_cliente_id;
END IF;

    IF p_receta_id IS NOT NULL THEN
UPDATE recetas SET venta_id = v_venta_id WHERE id = p_receta_id;
END IF;

RETURN QUERY SELECT v_venta_id, v_numero_factura;
END;
$$;