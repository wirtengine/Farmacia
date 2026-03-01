package com.farmacia.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DevolucionResponseDTO {
    private Long id;
    private Long ventaId;
    private String numeroFactura;
    private Long detalleId;
    private String medicamentoNombre;
    private Integer cantidad;
    private String motivo;
    private String estado;
    private LocalDateTime fechaSolicitud;
    private String vendedorNombre;
    private String adminNombre;
    private LocalDateTime fechaAprobacion;
    private String observacionAdmin;
}