package com.farmacia.sanidadbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RackResponse {

    private Long id;

    private String nombre;

    private String descripcion;

    private Integer ancho;

    private Integer alto;

    private Integer profundidad;

    private Boolean activo;
}