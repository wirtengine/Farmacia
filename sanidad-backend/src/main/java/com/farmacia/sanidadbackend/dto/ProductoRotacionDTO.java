package com.farmacia.sanidadbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ProductoRotacionDTO {
    private String nombre;
    private int unidadesVendidas;
    private int cantidadVentas;
    private BigDecimal totalGenerado;
}