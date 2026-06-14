CREATE OR REPLACE VIEW vw_productos_mayor_rotacion AS
SELECT
    m.id AS medicamento_id,
    m.nombre AS nombre,
    COALESCE(SUM(vd.cantidad), 0)::integer AS unidades_vendidas,
    COUNT(DISTINCT v.id)::integer AS cantidad_ventas,
    COALESCE(SUM(vd.subtotal), 0) AS total_generado
FROM medicamentos m
         JOIN lote_detalle ld
              ON ld.medicamento_id = m.id
         JOIN venta_detalle vd
              ON vd.lote_detalle_id = ld.id
         JOIN ventas v
              ON v.id = vd.venta_id
WHERE m.activo = true
  AND v.activo = true
GROUP BY m.id, m.nombre
ORDER BY unidades_vendidas DESC, total_generado DESC;