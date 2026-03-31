package com.farmacia.sanidadbackend.inteligencia.assistant;

import com.farmacia.sanidadbackend.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final GeminiClient geminiClient;

    public String consultarIAConContexto(String query, Usuario usuario, String contexto) {
        String prompt = """
Eres un asistente experto en farmacia.

ROL:
%s

CONSULTA:
%s

DATOS DEL SISTEMA:
%s

Responde con análisis claro, recomendaciones y acciones.
""".formatted(usuario.getRol(), query, contexto);

        return geminiClient.generate(prompt);
    }
}