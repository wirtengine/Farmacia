package com.farmacia.sanidadbackend.inteligencia.perdidas.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductoVencidoDTO {
    private Long loteId;
    private String numeroLote;
    private LocalDate fechaVencimiento;
    private Long medicamentoId;
    private String medicamentoNombre;
    private Integer cantidadVencida;
    private BigDecimal valorPerdido;
}
