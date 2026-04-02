package com.farmacia.sanidadbackend.inteligencia.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
public class GeminiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Envía una solicitud a Gemini con historial y herramientas.
     * @param contents Lista de contenidos (historial de mensajes)
     * @param tools Lista de herramientas (function declarations)
     * @return Respuesta encapsulada (texto o llamada a función)
     */
    public GeminiResponse generateWithTools(List<Map<String, Object>> contents, List<Map<String, Object>> tools) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        // Construir el cuerpo de la solicitud
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);
        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", Map.of("functionDeclarations", tools));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String body = objectMapper.writeValueAsString(requestBody);
            log.debug("Solicitud a Gemini: {}", body);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidate = root.path("candidates").get(0);
                JsonNode content = candidate.path("content");
                JsonNode parts = content.path("parts");

                // Verificar si es una llamada a función
                for (JsonNode part : parts) {
                    if (part.has("functionCall")) {
                        JsonNode functionCall = part.get("functionCall");
                        String name = functionCall.get("name").asText();
                        Map<String, Object> args = objectMapper.convertValue(functionCall.get("args"), Map.class);
                        return GeminiResponse.functionCall(name, args);
                    }
                }

                // Si no, extraer texto
                String text = parts.get(0).path("text").asText();
                return GeminiResponse.text(text);
            } else {
                log.error("Error en Gemini: HTTP {}", response.getStatusCode());
                return GeminiResponse.text("Error al comunicarse con el asistente. Código: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Excepción en GeminiClient", e);
            return GeminiResponse.text("Error interno: " + e.getMessage());
        }
    }

    /**
     * Método simplificado para uso directo sin herramientas (compatibilidad)
     */
    public String generate(String prompt) {
        List<Map<String, Object>> contents = List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt))));
        GeminiResponse resp = generateWithTools(contents, null);
        return resp.isText() ? resp.getText() : "Error: se esperaba respuesta de texto";
    }

    // Clase interna para encapsular la respuesta
    public static class GeminiResponse {
        private final boolean isFunctionCall;
        private final String text;
        private final String functionName;
        private final Map<String, Object> functionArgs;

        private GeminiResponse(boolean isFunctionCall, String text, String functionName, Map<String, Object> functionArgs) {
            this.isFunctionCall = isFunctionCall;
            this.text = text;
            this.functionName = functionName;
            this.functionArgs = functionArgs;
        }

        public static GeminiResponse text(String text) {
            return new GeminiResponse(false, text, null, null);
        }

        public static GeminiResponse functionCall(String name, Map<String, Object> args) {
            return new GeminiResponse(true, null, name, args);
        }

        public boolean isFunctionCall() { return isFunctionCall; }
        public boolean isText() { return !isFunctionCall; }
        public String getText() { return text; }
        public String getFunctionName() { return functionName; }
        public Map<String, Object> getFunctionArgs() { return functionArgs; }
    }
}