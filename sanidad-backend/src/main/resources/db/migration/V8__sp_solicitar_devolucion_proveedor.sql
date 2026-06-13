CREATE OR REPLACE FUNCTION fn_solicitar_devolucion_proveedor(
    p_lote_id BIGINT,
    p_solicitado_por_id BIGINT,
    p_detalles JSONB,
    p_motivo VARCHAR DEFAULT NULL   -- <- Parámetro con valor por defecto va al FINAL
)
RETURNS TABLE(devolucion_id BIGINT)
LANGUAGE plpgsql
AS $$
DECLARE
v_lote RECORD;
    v_devolucion_id BIGINT;
    detalle RECORD;
BEGIN
    -- Verificar lote
SELECT * INTO v_lote FROM lotes WHERE id = p_lote_id AND activo = true;
IF NOT FOUND THEN
        RAISE EXCEPTION 'Lote no encontrado o inactivo';
END IF;

    -- Verificar usuario
    PERFORM id FROM usuarios WHERE id = p_solicitado_por_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario no encontrado';
END IF;

    -- Insertar la devolución a proveedor
INSERT INTO devoluciones_proveedor (lote_id, proveedor_id, solicitado_por_id, estado, motivo, fecha_solicitud)
VALUES (p_lote_id, v_lote.proveedor_id, p_solicitado_por_id, 'PENDIENTE', p_motivo, NOW())
    RETURNING id INTO v_devolucion_id;

-- Procesar cada detalle
FOR detalle IN SELECT * FROM jsonb_to_recordset(p_detalles) AS x("loteDetalleId" BIGINT, "cantidadDevuelta" INT)
    LOOP
        -- Validar que el detalle pertenece al lote
        IF NOT EXISTS (
            SELECT 1 FROM lote_detalle
            WHERE id = detalle."loteDetalleId" AND lote_id = p_lote_id
        ) THEN
            RAISE EXCEPTION 'El detalle % no pertenece al lote seleccionado', detalle."loteDetalleId";
END IF;

        -- Insertar detalle de la devolución
INSERT INTO devolucion_proveedor_detalle (devolucion_proveedor_id, lote_detalle_id, cantidad_devuelta)
VALUES (v_devolucion_id, detalle."loteDetalleId", detalle."cantidadDevuelta");
END LOOP;

RETURN QUERY SELECT v_devolucion_id;
END;
$$;