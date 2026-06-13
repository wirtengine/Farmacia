-- Tabla para historial de precios
CREATE TABLE IF NOT EXISTS precios_historial (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 medicamento_id BIGINT NOT NULL,
                                                 precio_anterior NUMERIC(10,2),
    precio_nuevo NUMERIC(10,2) NOT NULL,
    fecha_cambio TIMESTAMPTZ DEFAULT NOW(),
    usuario_cambio TEXT DEFAULT current_user
    );

-- Función trigger
CREATE OR REPLACE FUNCTION fn_auditar_cambio_precio()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.precio_unitario IS DISTINCT FROM NEW.precio_unitario THEN
        INSERT INTO precios_historial (medicamento_id, precio_anterior, precio_nuevo)
        VALUES (NEW.id, OLD.precio_unitario, NEW.precio_unitario);
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger
CREATE TRIGGER trg_auditar_precio_medicamento
    AFTER UPDATE ON medicamentos
    FOR EACH ROW
    EXECUTE FUNCTION fn_auditar_cambio_precio();