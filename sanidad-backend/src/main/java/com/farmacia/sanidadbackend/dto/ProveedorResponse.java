package com.farmacia.sanidadbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProveedorResponse {

    private Long id;

    private String ruc;

    private String nombre;

    private String telefono;

    private String email;

    private boolean activo;

}