package com.farmacia.sanidadbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductoStockDTO {
    private String nombre;
    private int stockTotal;
}