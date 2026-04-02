package com.farmacia.sanidadbackend.inteligencia.assistant;

import com.farmacia.sanidadbackend.model.Rol;
import com.farmacia.sanidadbackend.model.Usuario;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ToolDefinitions {

    // Definición completa de todas las funciones disponibles
    private static final List<Map<String, Object>> ALL_FUNCTIONS = List.of(
            // Funciones originales
            functionDefinition("obtener_ventas_por_periodo",
                    "Obtiene el total de ventas y número de ventas en un rango de fechas. Si se proporciona idUsuario, filtra por vendedor.",
                    Map.of(
                            "fechaInicio", Map.of("type", "STRING", "description", "Fecha inicio (YYYY-MM-DD)"),
                            "fechaFin", Map.of("type", "STRING", "description", "Fecha fin (YYYY-MM-DD)"),
                            "idUsuario", Map.of("type", "INTEGER", "description", "ID del vendedor (opcional)")
                    ),
                    List.of("fechaInicio", "fechaFin")),

            functionDefinition("obtener_productos_bajo_stock",
                    "Lista productos con stock por debajo del umbral (por defecto 10 unidades).",
                    Map.of(
                            "umbral", Map.of("type", "INTEGER", "description", "Umbral de stock mínimo (opcional, default 10)")
                    ),
                    List.of()),

            functionDefinition("obtener_detalles_cliente",
                    "Obtiene la información de un cliente por su ID o cédula.",
                    Map.of(
                            "id", Map.of("type", "INTEGER", "description", "ID del cliente (opcional)"),
                            "cedula", Map.of("type", "STRING", "description", "Cédula del cliente (opcional)")
                    ),
                    List.of()),

            functionDefinition("obtener_stock_actual",
                    "Obtiene el stock actual de un medicamento por su nombre o ID.",
                    Map.of(
                            "nombre", Map.of("type", "STRING", "description", "Nombre del medicamento (opcional)"),
                            "id", Map.of("type", "INTEGER", "description", "ID del medicamento (opcional)")
                    ),
                    List.of()),

            functionDefinition("sugerir_reorden",
                    "Analiza el stock actual y las ventas para sugerir productos que deberían reordenarse.",
                    Map.of(
                            "umbralDias", Map.of("type", "INTEGER", "description", "Días de inventario a considerar (opcional, default 30)")
                    ),
                    List.of()),

            functionDefinition("obtener_ranking_vendedores",
                    "Obtiene el ranking de vendedores por total de ventas (solo ADMIN).",
                    Map.of(
                            "periodo", Map.of("type", "STRING", "description", "Período: 'mensual', 'anual' o 'historico' (opcional, default 'historico')")
                    ),
                    List.of()),

            // ================== NUEVAS FUNCIONES ==================

            functionDefinition("generar_reporte_ventas",
                    "Genera un archivo PDF o Excel con el listado detallado de todas las ventas en un rango de fechas. Solo ADMIN.",
                    Map.of(
                            "fechaInicio", Map.of("type", "STRING", "description", "Fecha inicio (YYYY-MM-DD)"),
                            "fechaFin", Map.of("type", "STRING", "description", "Fecha fin (YYYY-MM-DD)"),
                            "formato", Map.of("type", "STRING", "description", "Formato: PDF o EXCEL")
                    ),
                    List.of("fechaInicio", "fechaFin", "formato")),

            functionDefinition("predecir_ventas",
                    "Predice las ventas futuras de un medicamento usando regresión lineal. Solo ADMIN.",
                    Map.of(
                            "medicamentoId", Map.of("type", "INTEGER", "description", "ID del medicamento (opcional)"),
                            "nombre", Map.of("type", "STRING", "description", "Nombre del medicamento (opcional)"),
                            "dias", Map.of("type", "INTEGER", "description", "Número de días a predecir (default 7)")
                    ),
                    List.of()),

            functionDefinition("obtener_sugerencias_negocio",
                    "Analiza los datos del sistema (stock bajo, ventas recientes, productos inmovilizados) y devuelve sugerencias prácticas para mejorar el negocio. Solo ADMIN.",
                    Map.of(), // Sin parámetros necesarios
                    List.of())
    );

    /**
     * Retorna las herramientas disponibles según el rol del usuario.
     * ADMIN tiene acceso a todas, VENDEDOR solo a las que no sean confidenciales.
     */
    public List<Map<String, Object>> getToolsForUser(Usuario usuario) {
        if (usuario.getRol() == Rol.ADMIN) {
            return ALL_FUNCTIONS;
        } else {
            // Excluir funciones que solo admin puede ver
            return ALL_FUNCTIONS.stream()
                    .filter(f -> !Set.of(
                            "obtener_ranking_vendedores",
                            "generar_reporte_ventas",
                            "predecir_ventas",
                            "obtener_sugerencias_negocio"
                    ).contains(f.get("name")))
                    .toList();
        }
    }

    // Helper para construir la definición de una función según el formato de Gemini
    private static Map<String, Object> functionDefinition(String name, String description, Map<String, Object> properties, List<String> required) {
        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "OBJECT");
        parameters.put("properties", properties);
        if (!required.isEmpty()) {
            parameters.put("required", required);
        }
        function.put("parameters", parameters);
        return function;
    }
}