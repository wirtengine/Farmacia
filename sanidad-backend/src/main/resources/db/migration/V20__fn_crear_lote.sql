CREATE OR REPLACE FUNCTION fn_crear_lote(
    p_fecha_fabricacion DATE,
    p_fecha_vencimiento DATE,
    p_proveedor_id BIGINT,
    p_factura VARCHAR,
    p_detalles JSONB
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
v_lote_id BIGINT;
    v_numero_lote VARCHAR;
    detalle RECORD;
BEGIN
    PERFORM id FROM proveedores WHERE id = p_proveedor_id AND activo = true;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Proveedor no encontrado o inactivo';
END IF;

    v_numero_lote := 'LOT-' || to_char(CURRENT_DATE, 'YYYYMMDD') || '-' || lpad(floor(random()*9000+1000)::text, 4, '0');

INSERT INTO lotes (numero_lote, fecha_fabricacion, fecha_vencimiento, proveedor_id, factura, activo)
VALUES (v_numero_lote, p_fecha_fabricacion, p_fecha_vencimiento, p_proveedor_id, p_factura, true)
    RETURNING id INTO v_lote_id;

FOR detalle IN SELECT * FROM jsonb_to_recordset(p_detalles) AS x("medicamentoId" BIGINT, "cantidad" INT, "rackId" BIGINT, "nivel" INT, "columna" INT, "profundidadIndex" INT)
    LOOP
        PERFORM id FROM medicamentos WHERE id = detalle."medicamentoId" AND activo = true;
IF NOT FOUND THEN
            RAISE EXCEPTION 'Medicamento con ID % no encontrado o inactivo', detalle."medicamentoId";
END IF;

INSERT INTO lote_detalle (lote_id, medicamento_id, cantidad)
VALUES (v_lote_id, detalle."medicamentoId", detalle."cantidad");

IF detalle."rackId" IS NOT NULL THEN
            INSERT INTO ubicaciones_lote (rack_id, lote_detalle_id, nivel, columna, profundidad_index, cantidad, activo)
            VALUES (detalle."rackId", currval('lote_detalle_id_seq'), detalle."nivel", detalle."columna", detalle."profundidadIndex", detalle."cantidad", true);
END IF;
END LOOP;

RETURN v_lote_id;
END;
$$;