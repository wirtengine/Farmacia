package com.farmacia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ProveedorRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "^[0-9]{14}$", message = "El RUC debe tener 14 dígitos numéricos")
    private String ruc;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "El teléfono debe tener 8 dígitos")
    private String telefono;

    @Email(message = "Debe ser un email válido")
    private String email;

    private String direccion;

    private String contacto;

    @Pattern(regexp = "^[0-9]{8}$", message = "El teléfono de contacto debe tener 8 dígitos")
    private String telefonoContacto;
}