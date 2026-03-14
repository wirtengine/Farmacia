package com.farmacia.sanidadbackend.dto;

import com.farmacia.sanidadbackend.model.Rol;
import lombok.Data;

@Data
public class UsuarioRequest {
    private String username;
    private String password;
    private Rol rol;  // ADMIN o VENDEDOR
}