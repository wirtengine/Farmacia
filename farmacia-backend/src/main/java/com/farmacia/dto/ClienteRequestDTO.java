package com.farmacia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ClienteRequestDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotBlank(message = "La identificación es obligatoria")
    @Pattern(regexp = "^[A-Za-z0-9]{6,20}$", message = "La identificación debe tener entre 6 y 20 caracteres alfanuméricos")
    private String identificacion;

    @Pattern(regexp = "^[0-9]{8,15}$", message = "El teléfono debe tener entre 8 y 15 dígitos")
    private String telefono;

    @Email(message = "Debe ser un email válido")
    private String email;

    private String direccion;

    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    private String observaciones;
}