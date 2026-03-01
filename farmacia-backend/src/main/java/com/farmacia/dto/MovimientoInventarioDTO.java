package com.farmacia.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MovimientoInventarioDTO {
    private Long id;
    private Long loteId;
    private String numeroLote;
    private Long medicamentoId;
    private String medicamentoNombre;
    private String tipo;
    private Integer cantidad;
    private LocalDateTime fecha;
    private String motivo;
    private String usuarioNombre;
}