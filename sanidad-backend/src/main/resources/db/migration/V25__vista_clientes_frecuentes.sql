CREATE OR REPLACE VIEW vw_clientes_frecuentes AS
SELECT
    c.id AS cliente_id,
    c.nombre AS nombre_cliente,
    COUNT(v.id)::integer AS cantidad_compras,
    COALESCE(SUM(v.total), 0) AS total_gastado,
    MAX(v.fecha) AS ultima_compra
FROM clientes c
         JOIN ventas v
              ON v.cliente_id = c.id
WHERE c.activo = true
  AND v.activo = true
GROUP BY c.id, c.nombre
ORDER BY cantidad_compras DESC, total_gastado DESC;