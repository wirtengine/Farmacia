package com.farmacia.sanidadbackend.inteligencia.perdidas.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductoInmovilDTO {
    private Long medicamentoId;
    private String medicamentoNombre;
    private Integer stockActual;
    private Integer ventasUltimos90Dias;
    private Integer diasSinMovimiento; // opcional
    private BigDecimal valorInmovilizado;
}