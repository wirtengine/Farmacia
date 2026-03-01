package com.farmacia.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MedicamentoResponseDTO {
    private Long id;
    private String nombre;
    private String principioActivo;
    private String presentacion;
    private String viaAdministracion;
    private String fabricante;          // ya no lleva @NotBlank
    private String registroSanitario;
    private Boolean requiereReceta;
    private String tipoVenta;
    private BigDecimal precioVenta;
    private Integer stockMinimo;
    private Integer stockMaximo;
    private Boolean activo;
    private Integer stockTotal;          // <-- NUEVO CAMPO
}