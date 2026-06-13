CREATE TABLE IF NOT EXISTS ventas_anuladas (
                                               id BIGSERIAL PRIMARY KEY,
                                               venta_id BIGINT NOT NULL,
                                               fecha_anulacion TIMESTAMPTZ DEFAULT NOW(),
    usuario_anulacion TEXT DEFAULT current_user,
    datos_venta JSONB
    );

CREATE OR REPLACE FUNCTION fn_registrar_venta_anulada()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.activo = false AND OLD.activo = true THEN
        INSERT INTO ventas_anuladas (venta_id, datos_venta)
        VALUES (NEW.id, to_jsonb(OLD));
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_venta_anulada
    AFTER UPDATE ON ventas
    FOR EACH ROW
    EXECUTE FUNCTION fn_registrar_venta_anulada();