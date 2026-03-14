package com.farmacia.sanidadbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class LoteResponse {

    private Long id;

    private String numeroLote;

    private LocalDate fechaFabricacion;

    private LocalDate fechaVencimiento;

    // Datos del proveedor
    private Long proveedorId;

    private String proveedorNombre;

    private String proveedorRuc;

    private String factura;

    private boolean activo;

    private List<LoteDetalleResponse> detalles;
}