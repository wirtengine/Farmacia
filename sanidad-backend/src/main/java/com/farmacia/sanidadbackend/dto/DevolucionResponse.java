package com.farmacia.sanidadbackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DevolucionResponse {

    private Long id;
    private String numeroDevolucion;
    private Long ventaId;
    private String numeroFactura;

    private Long usuarioSolicitanteId;
    private String usuarioSolicitanteNombre;

    private Long usuarioApruebaId;
    private String usuarioApruebaNombre;

    private String estado; // PENDIENTE, APROBADA, RECHAZADA
    private String motivo;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaAprobacion;

    private BigDecimal subtotalDevuelto;
    private BigDecimal ivaDevuelto;
    private BigDecimal totalDevuelto;

    private BigDecimal montoDevueltoEfectivo;
    private BigDecimal montoDevueltoSaldo;

    private List<DevolucionDetalleResponse> detalles;
}