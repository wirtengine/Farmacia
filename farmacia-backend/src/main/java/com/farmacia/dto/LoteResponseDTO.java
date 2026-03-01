package com.farmacia.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LoteResponseDTO {
    private Long id;
    private String numeroLote;
    private Long medicamentoId;
    private String medicamentoNombre;
    private String medicamentoPresentacion;
    private LocalDate fechaFabricacion;
    private LocalDate fechaVencimiento;
    private Integer cantidadInicial;
    private Integer cantidadActual;
    private String fabricante;
    private String proveedor;
    private LocalDateTime fechaIngreso;
    private Boolean activo;
    private String estado; // "VIGENTE", "PRÓXIMO A VENCER", "VENCIDO"
}