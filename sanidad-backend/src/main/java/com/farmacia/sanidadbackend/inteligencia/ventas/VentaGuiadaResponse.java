package com.farmacia.sanidadbackend.inteligencia.ventas;

import com.farmacia.sanidadbackend.dto.SugerenciaProductoDTO;
import lombok.Data;

import java.util.List;

@Data
public class VentaGuiadaResponse {
    private SugerenciaProductoDTO loteRecomendado;
    private List<SugerenciaProductoDTO> complementarios;
    private List<SugerenciaProductoDTO> productosFrecuentesCliente;
    private List<String> ultimasCompras;
}