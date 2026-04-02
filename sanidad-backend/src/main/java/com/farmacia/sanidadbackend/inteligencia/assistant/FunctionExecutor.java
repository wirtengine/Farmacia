package com.farmacia.sanidadbackend.inteligencia.assistant;

import com.farmacia.sanidadbackend.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FunctionExecutor {

    private final AssistantService assistantService;

    /**
     * Ejecuta la función solicitada por Gemini.
     * @param functionName Nombre de la función (coincide con el definido en ToolDefinitions)
     * @param args Argumentos de la función (Map con los valores)
     * @param usuario Usuario que hace la consulta (para validar permisos)
     * @return Resultado de la función (puede ser Map, List, etc., se serializará a JSON)
     */
    public Object execute(String functionName, Map<String, Object> args, Usuario usuario) {
        log.info("Ejecutando función: {} con argumentos: {}", functionName, args);
        switch (functionName) {
            case "obtener_ventas_por_periodo":
                return assistantService.obtenerVentasPorPeriodo(args, usuario);

            case "obtener_productos_bajo_stock":
                return assistantService.obtenerProductosBajoStock(args, usuario);

            case "obtener_detalles_cliente":
                return assistantService.obtenerDetallesCliente(args, usuario);

            case "obtener_stock_actual":
                return assistantService.obtenerStockActual(args, usuario);

            case "sugerir_reorden":
                return assistantService.sugerirReorden(args, usuario);

            case "obtener_ranking_vendedores":
                return assistantService.obtenerRankingVendedores(args, usuario);

            // ✅ NUEVO: Generar reporte
            case "generar_reporte_ventas":
                return assistantService.generarReporteVentas(args, usuario);

            // ✅ NUEVO: Predicción de ventas
            case "predecir_ventas":
                return assistantService.predecirVentasMedicamento(args, usuario);

            default:
                throw new IllegalArgumentException("Función no soportada: " + functionName);
        }
    }
}