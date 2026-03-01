package com.farmacia.dto;

import lombok.Data;

@Data
public class ProveedorResponseDTO {
    private Long id;
    private String nombre;
    private String ruc;
    private String telefono;
    private String email;
    private String direccion;
    private String contacto;
    private String telefonoContacto;
    private Boolean activo;
}