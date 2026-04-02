package com.farmacia.sanidadbackend.inteligencia.assistant;

import com.farmacia.sanidadbackend.inteligencia.assistant.GeminiClient.GeminiResponse;
import com.farmacia.sanidadbackend.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final GeminiClient geminiClient;
    private final FunctionExecutor functionExecutor;
    private final ToolDefinitions toolDefinitions;
    private final AssistantService assistantService;

    public String procesarConsulta(String consulta, Usuario usuario) {
        // Construir contexto general
        String contexto = assistantService.construirContexto(usuario);

        List<Map<String, Object>> contents = new ArrayList<>();

        // Prompt del sistema: indicamos explícitamente el rol y las herramientas
        String systemPrompt = String.format("""
            Eres un asistente experto en farmacia llamado FarmaSystem Assistant.
            
            ROL DEL USUARIO ACTUAL: %s
            IMPORTANTE: Este usuario tiene el rol '%s'. Si es ADMIN, tiene todos los permisos.
            
            HERRAMIENTAS DISPONIBLES:
            - generar_reporte_ventas: Para generar archivos PDF o Excel con detalles de ventas.
            - predecir_ventas: Para predicciones de ventas.
            - obtener_sugerencias_negocio: Para recomendaciones de mejora.
            - obtener_ventas_por_periodo, obtener_productos_bajo_stock, etc.
            
            INSTRUCCIONES OBLIGATORIAS:
            1. Cuando el usuario pida "generar un reporte", "PDF", "Excel", "exportar ventas", DEBES llamar a la función 'generar_reporte_ventas' con los parámetros adecuados.
            2. No digas que no tienes permiso si el rol es ADMIN. El sistema ya validará permisos en la ejecución de la función.
            3. Usa el contexto solo para respuestas generales; para datos específicos o archivos, usa las herramientas.
            
            CONTEXTO ACTUAL DEL SISTEMA:
            %s
            
            Responde de forma útil y profesional.
            """, usuario.getRol(), usuario.getRol(), contexto);

        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", systemPrompt))));
        contents.add(Map.of("role", "model", "parts", List.of(Map.of("text", "Entendido. Usaré las herramientas cuando sea necesario."))));
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", consulta))));

        List<Map<String, Object>> tools = toolDefinitions.getToolsForUser(usuario);

        int maxIterations = 5;
        for (int i = 0; i < maxIterations; i++) {
            GeminiResponse response = geminiClient.generateWithTools(contents, tools);
            if (response.isText()) {
                return response.getText();
            } else if (response.isFunctionCall()) {
                Object result;
                try {
                    result = functionExecutor.execute(response.getFunctionName(), response.getFunctionArgs(), usuario);
                } catch (Exception e) {
                    log.error("Error ejecutando función " + response.getFunctionName(), e);
                    result = Map.of("error", "Error interno: " + e.getMessage());
                }
                Map<String, Object> functionResponsePart = Map.of(
                        "functionResponse", Map.of(
                                "name", response.getFunctionName(),
                                "response", Map.of("result", result)
                        )
                );
                contents.add(Map.of("role", "function", "parts", List.of(functionResponsePart)));
            } else {
                return "No se pudo procesar la respuesta.";
            }
        }
        return "Se excedió el número máximo de llamadas.";
    }
}