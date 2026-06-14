package com.farmacia.sanidadbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProductoSinMovimientoDTO {
    private String nombre;
    private int stockActual;
    private LocalDateTime ultimaFechaVenta;
    private int unidadesVendidas;
}