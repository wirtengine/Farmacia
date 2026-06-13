CREATE OR REPLACE FUNCTION fn_asignar_ubicacion(
    p_lote_detalle_id BIGINT,
    p_rack_id BIGINT,
    p_nivel INT,
    p_columna INT,
    p_profundidad_index INT,
    p_cantidad INT
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
v_rack RECORD;
    v_lote_detalle RECORD;
    v_ya_asignado INT;
    v_nuevo_total INT;
    v_ubicacion_id BIGINT;
BEGIN
    -- Validar rack
SELECT * INTO v_rack FROM racks WHERE id = p_rack_id AND activo = true;
IF NOT FOUND THEN
        RAISE EXCEPTION 'Rack no encontrado o inactivo';
END IF;

    -- Validar lote detalle y que el lote esté activo
SELECT ld.*, l.activo AS lote_activo
INTO v_lote_detalle
FROM lote_detalle ld
         JOIN lotes l ON ld.lote_id = l.id
WHERE ld.id = p_lote_detalle_id;
IF NOT FOUND OR v_lote_detalle.lote_activo = false THEN
        RAISE EXCEPTION 'LoteDetalle no encontrado o lote inactivo';
END IF;

    -- Validar límites del rack
    IF p_nivel >= v_rack.alto OR p_nivel < 0 OR
       p_columna >= v_rack.ancho OR p_columna < 0 OR
       p_profundidad_index >= v_rack.profundidad OR p_profundidad_index < 0 THEN
        RAISE EXCEPTION 'Coordenadas fuera del rango del rack';
END IF;

    -- Verificar que la celda no esté ocupada (bloqueo implícito con unique constraint, pero validamos antes)
    PERFORM 1 FROM ubicaciones_lote
    WHERE rack_id = p_rack_id
      AND nivel = p_nivel
      AND columna = p_columna
      AND profundidad_index = p_profundidad_index
      AND activo = true;
    IF FOUND THEN
        RAISE EXCEPTION 'La celda ya está ocupada';
END IF;

    -- Validar stock global (tu lógica)
SELECT COALESCE(SUM(cantidad), 0) INTO v_ya_asignado
FROM ubicaciones_lote
WHERE lote_detalle_id = p_lote_detalle_id AND activo = true;

v_nuevo_total := v_ya_asignado + p_cantidad;
    IF v_nuevo_total > v_lote_detalle.cantidad THEN
        RAISE EXCEPTION 'Stock insuficiente. Ya hay % unidades asignadas. Solo quedan % disponibles.',
            v_ya_asignado, v_lote_detalle.cantidad - v_ya_asignado;
END IF;

    -- Insertar ubicación
INSERT INTO ubicaciones_lote (rack_id, lote_detalle_id, nivel, columna, profundidad_index, cantidad, activo)
VALUES (p_rack_id, p_lote_detalle_id, p_nivel, p_columna, p_profundidad_index, p_cantidad, true)
    RETURNING id INTO v_ubicacion_id;

RETURN v_ubicacion_id;
END;
$$;