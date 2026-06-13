CREATE OR REPLACE FUNCTION fn_aprobar_devolucion_proveedor(
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
    v_numero_devolucion VARCHAR;
BEGIN
SELECT * INTO v_devolucion FROM devoluciones_proveedor WHERE id = p_devolucion_id;
IF NOT FOUND THEN
        RAISE EXCEPTION 'Devolución no encontrada';
END IF;

    IF v_devolucion.estado != 'PENDIENTE' THEN
        RAISE EXCEPTION 'La devolución ya fue procesada';
END IF;

    -- Rechazo
    IF p_aprobada = FALSE THEN
UPDATE devoluciones_proveedor
SET estado = 'RECHAZADA',
    aprobado_por_id = p_aprobado_por_id,
    fecha_aprobacion = NOW(),
    motivo = COALESCE(p_motivo_rechazo, motivo)
WHERE id = p_devolucion_id;
RETURN QUERY SELECT p_devolucion_id, 'RECHAZADA'::VARCHAR;
RETURN;
END IF;

    -- Aprobación: generar número de devolución
    v_numero_devolucion := 'DPROV-' || to_char(CURRENT_DATE, 'YYYYMMDD') || '-' || floor(random() * 9000 + 1000)::text;

    -- Descontar stock de los lotes
UPDATE lote_detalle ld
SET cantidad = ld.cantidad - dd.cantidad_devuelta
    FROM devolucion_proveedor_detalle dd
WHERE dd.lote_detalle_id = ld.id
  AND dd.devolucion_proveedor_id = p_devolucion_id;

-- Actualizar la devolución
UPDATE devoluciones_proveedor
SET estado = 'APROBADA',
    numero_devolucion = v_numero_devolucion,
    aprobado_por_id = p_aprobado_por_id,
    fecha_aprobacion = NOW()
WHERE id = p_devolucion_id;

RETURN QUERY SELECT p_devolucion_id, 'APROBADA'::VARCHAR;
END;
$$;