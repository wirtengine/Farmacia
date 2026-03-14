package com.farmacia.sanidadbackend.dto;

import com.farmacia.sanidadbackend.model.Rol;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class ActualizarUsuarioRequest {

    private String password;

    @NotNull
    private Rol rol;
}