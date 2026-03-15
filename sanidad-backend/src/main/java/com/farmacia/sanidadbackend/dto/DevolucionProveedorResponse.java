package com.farmacia.sanidadbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DevolucionProveedorResponse {

    private Long id;
    private String numeroDevolucion;
    private Long loteId;
    private String numeroFacturaLote; // lote.factura
    private Long proveedorId;
    private String proveedorNombre;
    private String proveedorTelefono;
    private String proveedorEmail;

    private Long solicitadoPorId;
    private String solicitadoPorNombre;

    private Long aprobadoPorId;
    private String aprobadoPorNombre;

    private String estado;
    private String motivo;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaAprobacion;

    private List<DevolucionProveedorDetalleResponse> detalles;
}