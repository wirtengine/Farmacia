CREATE OR REPLACE FUNCTION fn_auditar_devolucion()
RETURNS TRIGGER AS $$
BEGIN
INSERT INTO audit_log (table_name, operation, record_id, new_data, changed_at)
VALUES ('devoluciones', 'INSERT', NEW.id, to_jsonb(NEW), NOW());
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditar_devolucion
    AFTER INSERT ON devoluciones
    FOR EACH ROW
    EXECUTE FUNCTION fn_auditar_devolucion();