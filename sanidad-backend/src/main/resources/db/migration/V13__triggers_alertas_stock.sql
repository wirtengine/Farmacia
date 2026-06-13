CREATE TABLE IF NOT EXISTS alertas_stock (
                                             id BIGSERIAL PRIMARY KEY,
                                             lote_detalle_id BIGINT NOT NULL,
                                             medicamento_id BIGINT NOT NULL,
                                             stock_actual INT NOT NULL,
                                             fecha TIMESTAMPTZ DEFAULT NOW(),
    atendida BOOLEAN DEFAULT FALSE
    );

-- Función trigger
CREATE OR REPLACE FUNCTION fn_alerta_stock_bajo()
RETURNS TRIGGER AS $$
DECLARE
umbral INT := 10; -- Ajustable
BEGIN
    IF NEW.cantidad < umbral AND (OLD.cantidad IS NULL OR OLD.cantidad >= umbral) THEN
        INSERT INTO alertas_stock (lote_detalle_id, medicamento_id, stock_actual)
        VALUES (NEW.id, NEW.medicamento_id, NEW.cantidad);
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger sobre lote_detalle
CREATE TRIGGER trg_check_stock_bajo
    AFTER INSERT OR UPDATE ON lote_detalle
                        FOR EACH ROW
                        EXECUTE FUNCTION fn_alerta_stock_bajo();