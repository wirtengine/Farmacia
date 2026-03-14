package com.farmacia.sanidadbackend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClienteResponse {

    private Long id;
    private String cedula;
    private String nombre;
    private String telefono;
    private String email;
    private BigDecimal saldo;
    private Boolean activo;

}