CREATE OR REPLACE FUNCTION fn_auditar_receta()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado IS DISTINCT FROM NEW.estado THEN
        INSERT INTO audit_log (table_name, operation, record_id, old_data, new_data, changed_at)
        VALUES ('recetas', 'UPDATE', NEW.id, to_jsonb(OLD), to_jsonb(NEW), NOW());
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_receta_cambio_estado
    AFTER UPDATE ON recetas
    FOR EACH ROW
    EXECUTE FUNCTION fn_auditar_receta();