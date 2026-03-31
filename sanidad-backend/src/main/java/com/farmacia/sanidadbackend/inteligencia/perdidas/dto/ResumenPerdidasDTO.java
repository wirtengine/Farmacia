package com.farmacia.sanidadbackend.inteligencia.perdidas.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ResumenPerdidasDTO {
    private BigDecimal totalPerdidasVencimiento;
    private BigDecimal totalInmovilizado;
    private Integer cantidadProductosVencidos;
    private Integer cantidadProductosInmoviles;
    private Integer cantidadInconsistencias;
}