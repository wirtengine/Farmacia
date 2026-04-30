package com.farmacia.sanidadbackend.dto;

public interface StockMedicamentoDTO {
    Long getMedicamentoId();   // coincide con la columna "medicamento_id" de la función
    String getNombre();        // coincide con "nombre"
    Integer getStockTotal();   // coincide con "stock_total"
}