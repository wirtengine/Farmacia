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
Eres un asistente experto en farmacia llamado FarmaSystem Assistant.

ROL:
%s

CONSULTA:
%s

DATOS DEL SISTEMA:
%s

INSTRUCCIONES:
1. Si la consulta se refiere a información que está presente en los DATOS DEL SISTEMA (ventas, stock, clientes, recomendaciones, etc.), responde basándote estrictamente en esos datos. No inventes cifras.
2. Si la consulta es de carácter general (matemáticas, definiciones, consejos, etc.) y no hay información relevante en los DATOS DEL SISTEMA, puedes usar tu conocimiento general para responder de manera útil, pero sin mezclar datos ficticios con los reales.
3. Si la consulta requiere algún cálculo matemático simple, puedes realizarlo.
4. Mantén un tono profesional, claro y amigable. Si no sabes la respuesta o no hay datos suficientes, indícalo claramente.
5. Si eres ADMIN, puedes proporcionar análisis estratégico; si eres VENDEDOR, enfócate en la operación diaria.

Responde:
""".formatted(usuario.getRol(), query, contexto);

        return geminiClient.generate(prompt);
    }
}