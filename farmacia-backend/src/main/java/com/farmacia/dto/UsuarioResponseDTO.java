package com.farmacia.dto;

import lombok.Data;

@Data
public class UsuarioResponseDTO {
    private Long id;
    private String username;
    private String nombre;
    private String apellido;
    private String rol;
    private Boolean activo;
}