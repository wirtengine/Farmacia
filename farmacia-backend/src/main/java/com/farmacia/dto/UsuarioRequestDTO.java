package com.farmacia.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class UsuarioRequestDTO {

    @NotBlank
    private String username;

    // Contraseña actual → obligatoria solo cuando se crea el usuario
    private String password;

    // Nueva contraseña → opcional (se usa para recuperar o cambiar contraseña)
    private String newPassword;

    // Token enviado por correo para recuperar contraseña
    private String resetToken;

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank
    private String rol; // ADMIN o VENDEDOR
}