CREATE OR REPLACE FUNCTION fn_eliminar_ubicacion(
    p_ubicacion_id BIGINT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
v_ubicacion RECORD;
BEGIN
SELECT * INTO v_ubicacion FROM ubicaciones_lote WHERE id = p_ubicacion_id;
IF NOT FOUND THEN
        RAISE EXCEPTION 'Ubicación no encontrada';
END IF;

    -- Tu lógica solo marca como inactivo (baja lógica)
UPDATE ubicaciones_lote SET activo = false WHERE id = p_ubicacion_id;
END;
$$;