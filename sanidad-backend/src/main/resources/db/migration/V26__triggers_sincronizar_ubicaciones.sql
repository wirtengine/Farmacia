-- Sincronizacion de ubicaciones cuando el stock de un lote_detalle llega a 0.

CREATE OR REPLACE FUNCTION fn_sync_ubicaciones_lote_detalle()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.cantidad <= 0 THEN
UPDATE ubicaciones_lote
SET activo = false,
    cantidad = 0
WHERE lote_detalle_id = NEW.id
  AND activo = true;
END IF;

RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_ubicaciones_lote_detalle ON lote_detalle;

CREATE TRIGGER trg_sync_ubicaciones_lote_detalle
    AFTER UPDATE OF cantidad ON lote_detalle
    FOR EACH ROW
    EXECUTE FUNCTION fn_sync_ubicaciones_lote_detalle();


-- Sincronizacion de ubicaciones cuando un medicamento se desactiva.

CREATE OR REPLACE FUNCTION fn_sync_ubicaciones_medicamento()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.activo = false AND OLD.activo = true THEN
UPDATE ubicaciones_lote ul
SET activo = false,
    cantidad = 0
    FROM lote_detalle ld
WHERE ul.lote_detalle_id = ld.id
  AND ld.medicamento_id = NEW.id
  AND ul.activo = true;
END IF;

RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_ubicaciones_medicamento ON medicamentos;

CREATE TRIGGER trg_sync_ubicaciones_medicamento
    AFTER UPDATE OF activo ON medicamentos
    FOR EACH ROW
    EXECUTE FUNCTION fn_sync_ubicaciones_medicamento();


-- Sincronizacion de ubicaciones cuando un estante se desactiva.

CREATE OR REPLACE FUNCTION fn_sync_ubicaciones_rack()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.activo = false AND OLD.activo = true THEN
UPDATE ubicaciones_lote
SET activo = false,
    cantidad = 0
WHERE rack_id = NEW.id
  AND activo = true;
END IF;

RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_ubicaciones_rack ON racks;

CREATE TRIGGER trg_sync_ubicaciones_rack
    AFTER UPDATE OF activo ON racks
    FOR EACH ROW
    EXECUTE FUNCTION fn_sync_ubicaciones_rack();


-- Sincronizacion de ubicaciones cuando un lote completo se desactiva.

CREATE OR REPLACE FUNCTION fn_sync_ubicaciones_lote()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.activo = false AND OLD.activo = true THEN
UPDATE ubicaciones_lote ul
SET activo = false,
    cantidad = 0
    FROM lote_detalle ld
WHERE ul.lote_detalle_id = ld.id
  AND ld.lote_id = NEW.id
  AND ul.activo = true;
END IF;

RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_ubicaciones_lote ON lotes;

CREATE TRIGGER trg_sync_ubicaciones_lote
    AFTER UPDATE OF activo ON lotes
    FOR EACH ROW
    EXECUTE FUNCTION fn_sync_ubicaciones_lote();