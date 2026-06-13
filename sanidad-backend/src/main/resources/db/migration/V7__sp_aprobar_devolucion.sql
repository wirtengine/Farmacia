CREATE OR REPLACE FUNCTION fn_aprobar_devolucion(
    p_devolucion_id BIGINT,
    p_aprobado_por_id BIGINT,
    p_aprobada BOOLEAN,
    p_motivo_rechazo VARCHAR DEFAULT NULL
)
RETURNS TABLE(devolucion_id BIGINT, estado VARCHAR)
LANGUAGE plpgsql
AS $$
DECLARE
v_devolucion RECORD;
    v_factor NUMERIC(10, 2);
    v_saldo_devuelto NUMERIC(12, 2);
    v_efectivo_devuelto NUMERIC(12, 2);
    v_numero_devolucion VARCHAR;
    v_completa BOOLEAN;
BEGIN
SELECT * INTO v_devolucion FROM devoluciones WHERE id = p_devolucion_id;
IF NOT FOUND THEN
        RAISE EXCEPTION 'Devolución no encontrada';
END IF;

    IF v_devolucion.estado != 'PENDIENTE' THEN
        RAISE EXCEPTION 'La devolución ya fue procesada';
END IF;

    -- Rechazo
    IF p_aprobada = FALSE THEN
UPDATE devoluciones
SET estado = 'RECHAZADA',
    aprobado_por_id = p_aprobado_por_id,
    fecha_aprobacion = NOW(),
    motivo = COALESCE(p_motivo_rechazo, motivo)
WHERE id = p_devolucion_id;
RETURN QUERY SELECT p_devolucion_id, 'RECHAZADA'::VARCHAR;
RETURN;
END IF;

    -- Aprobación: generar número de devolución
    v_numero_devolucion := 'DEV-' || to_char(CURRENT_DATE, 'YYYYMMDD') || '-' || floor(random() * 9000 + 1000)::text;

    -- Reponer stock de cada detalle
UPDATE lote_detalle ld
SET cantidad = ld.cantidad + dd.cantidad_devuelta
    FROM devolucion_detalle dd
WHERE dd.lote_detalle_id = ld.id
  AND dd.devolucion_id = p_devolucion_id;

-- Calcular devolución de saldo y efectivo
SELECT CASE WHEN v.total > 0 THEN v_devolucion.total_devuelto / v.total ELSE 0 END
INTO v_factor
FROM ventas v
WHERE v.id = v_devolucion.venta_id;

v_saldo_devuelto := COALESCE((SELECT monto_usado_saldo FROM ventas WHERE id = v_devolucion.venta_id), 0) * v_factor;
    v_efectivo_devuelto := COALESCE((SELECT monto_efectivo FROM ventas WHERE id = v_devolucion.venta_id), 0) * v_factor;

    -- Devolver saldo al cliente
UPDATE clientes c
SET saldo = saldo + v_saldo_devuelto
    FROM ventas v
WHERE v.cliente_id = c.id
  AND v.id = v_devolucion.venta_id
  AND v_saldo_devuelto > 0;

-- Verificar si la devolución es completa y desactivar la venta
SELECT bool_and(dd.cantidad_devuelta >= vd.cantidad)
INTO v_completa
FROM devolucion_detalle dd
         JOIN venta_detalle vd ON dd.venta_detalle_id = vd.id
WHERE dd.devolucion_id = p_devolucion_id;

IF v_completa THEN
UPDATE ventas SET activo = FALSE WHERE id = v_devolucion.venta_id;
END IF;

    -- Actualizar la devolución
UPDATE devoluciones
SET estado = 'APROBADA',
    numero_devolucion = v_numero_devolucion,
    aprobado_por_id = p_aprobado_por_id,
    fecha_aprobacion = NOW(),
    monto_devuelto_saldo = v_saldo_devuelto,
    monto_devuelto_efectivo = v_efectivo_devuelto
WHERE id = p_devolucion_id;

RETURN QUERY SELECT p_devolucion_id, 'APROBADA'::VARCHAR;
END;
$$;