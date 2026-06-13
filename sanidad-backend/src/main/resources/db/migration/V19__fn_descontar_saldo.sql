CREATE OR REPLACE FUNCTION fn_descontar_saldo(
    p_cliente_id BIGINT,
    p_monto NUMERIC
)
RETURNS NUMERIC
LANGUAGE plpgsql
AS $$
DECLARE
v_saldo_actual NUMERIC;
    v_nuevo_saldo NUMERIC;
BEGIN
    -- Verificar que el cliente existe y está activo
SELECT saldo INTO v_saldo_actual
FROM clientes
WHERE id = p_cliente_id AND activo = true
    FOR UPDATE;

IF NOT FOUND THEN
        RAISE EXCEPTION 'Cliente no encontrado o inactivo';
END IF;

    -- Verificar que el saldo sea suficiente
    IF v_saldo_actual < p_monto THEN
        RAISE EXCEPTION 'Saldo insuficiente. Saldo actual: %', v_saldo_actual;
END IF;

    v_nuevo_saldo := v_saldo_actual - p_monto;

UPDATE clientes SET saldo = v_nuevo_saldo
WHERE id = p_cliente_id;

RETURN v_nuevo_saldo;
END;
$$;