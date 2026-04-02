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

    /**
     * Procesa una consulta del usuario, manejando el ciclo de llamadas a funciones.
     * @param consulta Texto de la consulta
     * @param usuario Usuario que realiza la consulta
     * @return Respuesta final en texto
     */
    public String procesarConsulta(String consulta, Usuario usuario) {
        // Construir historial de conversación. Podríamos mantener sesiones más adelante.
        List<Map<String, Object>> contents = new ArrayList<>();

        // Mensaje de sistema para dar contexto de rol y capacidades
        String systemPrompt = "Eres un asistente experto en farmacia llamado FarmaSystem Assistant. " +
                "Rol del usuario: " + usuario.getRol() + ". " +
                "Puedes usar las herramientas disponibles para obtener información actualizada del sistema. " +
                "Si no tienes suficientes datos para responder, indícalo claramente. " +
                "Sé conciso, profesional y amigable.";

        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", systemPrompt))));
        contents.add(Map.of("role", "model", "parts", List.of(Map.of("text", "Entendido. Estoy listo para ayudarte."))));
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", consulta))));

        // Obtener herramientas según rol
        List<Map<String, Object>> tools = toolDefinitions.getToolsForUser(usuario);

        // Bucle de llamadas a funciones (máximo 5 iteraciones para evitar ciclos)
        int maxIterations = 5;
        for (int i = 0; i < maxIterations; i++) {
            GeminiResponse response = geminiClient.generateWithTools(contents, tools);

            if (response.isText()) {
                return response.getText();
            } else if (response.isFunctionCall()) {
                // Ejecutar la función
                Object result;
                try {
                    result = functionExecutor.execute(response.getFunctionName(), response.getFunctionArgs(), usuario);
                } catch (Exception e) {
                    log.error("Error ejecutando función " + response.getFunctionName(), e);
                    result = Map.of("error", "Error interno al ejecutar la función: " + e.getMessage());
                }
                // Agregar la respuesta de la función al historial
                Map<String, Object> functionResponsePart = Map.of(
                        "functionResponse", Map.of(
                                "name", response.getFunctionName(),
                                "response", Map.of("result", result)
                        )
                );
                contents.add(Map.of("role", "function", "parts", List.of(functionResponsePart)));
            } else {
                // No debería ocurrir
                return "No se pudo procesar la respuesta del asistente.";
            }
        }
        return "Se excedió el número máximo de llamadas a funciones.";
    }

    /**
     * Método de compatibilidad con la interfaz anterior (si se usa en otros lugares)
     */
    public String consultarIAConContexto(String query, Usuario usuario, String contexto) {
        // Ignoramos el contexto, ahora usamos herramientas
        return procesarConsulta(query, usuario);
    }
}