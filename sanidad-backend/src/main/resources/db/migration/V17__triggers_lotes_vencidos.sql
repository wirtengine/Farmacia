CREATE OR REPLACE FUNCTION fn_desactivar_lote_vencido()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.fecha_vencimiento < CURRENT_DATE AND NEW.activo = true THEN
        NEW.activo := false;
INSERT INTO audit_log (table_name, operation, record_id, new_data, changed_at)
VALUES ('lotes', 'UPDATE', NEW.id, to_jsonb(NEW), NOW());
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_lote_vencido
    BEFORE INSERT OR UPDATE ON lotes
                         FOR EACH ROW
                         EXECUTE FUNCTION fn_desactivar_lote_vencido();