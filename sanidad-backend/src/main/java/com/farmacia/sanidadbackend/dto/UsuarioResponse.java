package com.farmacia.sanidadbackend.dto;

import com.farmacia.sanidadbackend.model.Rol;

import lombok.Data;

@Data
public class UsuarioResponse {

    private Long id;
    private String username;
    private Rol rol;
}