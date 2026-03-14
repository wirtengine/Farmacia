package com.farmacia.sanidadbackend.dto;

import com.farmacia.sanidadbackend.model.Medicamento;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MedicamentoResponse {

    private Long id;
    private String registroSanitario;
    private String nombre;
    private String presentacion;
    private String via;
    private String fabricante;
    private String tipoVenta;
    private BigDecimal precioUnitario;
    private Boolean receta;
    private Boolean activo;

    public static MedicamentoResponse fromEntity(Medicamento m) {
        MedicamentoResponse response = new MedicamentoResponse();

        response.setId(m.getId());
        response.setRegistroSanitario(m.getRegistroSanitario());
        response.setNombre(m.getNombre());
        response.setPresentacion(m.getPresentacion());
        response.setVia(m.getVia());
        response.setFabricante(m.getFabricante());
        response.setTipoVenta(m.getTipoVenta());
        response.setPrecioUnitario(m.getPrecioUnitario());
        response.setReceta(m.getReceta());
        response.setActivo(m.getActivo());

        return response;
    }
}