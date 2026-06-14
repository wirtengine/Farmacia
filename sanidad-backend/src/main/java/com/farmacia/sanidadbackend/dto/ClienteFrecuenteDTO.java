package com.farmacia.sanidadbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ClienteFrecuenteDTO {
    private String nombreCliente;
    private int cantidadCompras;
    private BigDecimal totalGastado;
    private LocalDateTime ultimaCompra;
}