package com.farmacia.sanidadbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClienteRequest {

    @NotBlank
    @Size(min = 8, max = 20)
    @Pattern(regexp = "^[0-9]+$", message = "La cédula debe contener solo números")
    private String cedula;

    @NotBlank
    @Size(min = 3, max = 100)
    private String nombre;

    @Size(max = 20)
    @Pattern(regexp = "^[0-9]+$", message = "El teléfono debe contener solo números")
    private String telefono;

    @Email
    @Size(max = 100)
    private String email;

    private BigDecimal saldo;
}