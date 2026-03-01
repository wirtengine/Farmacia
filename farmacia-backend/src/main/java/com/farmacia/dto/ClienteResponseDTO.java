package com.farmacia.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ClienteResponseDTO {
    private Long id;
    private String nombreCompleto;
    private String identificacion;
    private String telefono;
    private String email;
    private String direccion;
    private LocalDate fechaNacimiento;
    private LocalDateTime fechaRegistro;
    private Boolean activo;
    private String observaciones;
}