CREATE OR REPLACE FUNCTION fn_solicitar_devolucion(
    p_venta_id BIGINT,
    p_solicitado_por_id BIGINT,
    p_motivo VARCHAR,
    p_detalles JSONB
)
RETURNS TABLE(devolucion_id BIGINT, subtotal_devuelto NUMERIC(12,2), iva_devuelto NUMERIC(12,2), total_devuelto NUMERIC(12,2))
LANGUAGE plpgsql
AS $$
DECLARE
v_venta_activa BOOLEAN;
    v_subtotal NUMERIC(12,2) := 0;
    v_iva NUMERIC(12,2);
    v_total NUMERIC(12,2);
    detalle RECORD;
    v_venta_detalle_id BIGINT;
    v_cantidad_vendida INT;
    v_precio_unitario NUMERIC;
    v_subtotal_detalle NUMERIC(12,2);
    v_devolucion_id BIGINT;
BEGIN
    -- Verificar que la venta existe y está activa
SELECT activo INTO v_venta_activa FROM ventas WHERE id = p_venta_id;
IF NOT FOUND THEN RAISE EXCEPTION 'Venta no encontrada'; END IF;
    IF v_venta_activa = false THEN RAISE EXCEPTION 'La venta ya fue anulada'; END IF;

    -- Verificar usuario
    PERFORM id FROM usuarios WHERE id = p_solicitado_por_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Usuario no encontrado'; END IF;

    -- Insertar devolución (ahora con fecha_solicitud)
INSERT INTO devoluciones (venta_id, solicitado_por_id, estado, motivo, fecha_solicitud)
VALUES (p_venta_id, p_solicitado_por_id, 'PENDIENTE', p_motivo, NOW())
    RETURNING id INTO v_devolucion_id;

-- Procesar cada detalle
FOR detalle IN SELECT * FROM jsonb_to_recordset(p_detalles) AS x("ventaDetalleId" BIGINT, "cantidadDevuelta" INT)
    LOOP
SELECT vd.cantidad, vd.precio_unitario, vd.id
INTO v_cantidad_vendida, v_precio_unitario, v_venta_detalle_id
FROM venta_detalle vd WHERE vd.id = detalle."ventaDetalleId" AND vd.venta_id = p_venta_id;
IF NOT FOUND THEN RAISE EXCEPTION 'Detalle de venta no encontrado'; END IF;
        IF detalle."cantidadDevuelta" > v_cantidad_vendida THEN
            RAISE EXCEPTION 'Cantidad devuelta excede la vendida';
END IF;

        v_subtotal_detalle := v_precio_unitario * detalle."cantidadDevuelta";
INSERT INTO devolucion_detalle (devolucion_id, venta_detalle_id, lote_detalle_id, cantidad_devuelta, precio_unitario, subtotal)
SELECT v_devolucion_id, v_venta_detalle_id, vd.lote_detalle_id, detalle."cantidadDevuelta", v_precio_unitario, v_subtotal_detalle
FROM venta_detalle vd WHERE vd.id = v_venta_detalle_id;

v_subtotal := v_subtotal + v_subtotal_detalle;
END LOOP;

    -- Calcular totales
    v_iva := round(v_subtotal * 0.15, 2);
    v_total := v_subtotal + v_iva;

    -- Actualizar devolución con montos
UPDATE devoluciones SET subtotal_devuelto = v_subtotal, iva_devuelto = v_iva, total_devuelto = v_total
WHERE id = v_devolucion_id;

RETURN QUERY SELECT v_devolucion_id, v_subtotal, v_iva, v_total;
END;
$$;