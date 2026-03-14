package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class LoteRequest {

    private LocalDate fechaFabricacion;

    private LocalDate fechaVencimiento;

    @NotNull(message = "El proveedor es obligatorio")
    private Long proveedorId;

    private String factura;

    @NotEmpty(message = "Debe incluir al menos un medicamento")
    private List<LoteDetalleRequest> detalles;
}