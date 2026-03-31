package com.farmacia.sanidadbackend.inteligencia.assistant;

import com.farmacia.sanidadbackend.inteligencia.assistant.dto.AssistantResponse;
import com.farmacia.sanidadbackend.inteligencia.perdidas.PerdidasService;
import com.farmacia.sanidadbackend.inteligencia.recommendations.Recommendation;
import com.farmacia.sanidadbackend.inteligencia.recommendations.RecommendationPriority;
import com.farmacia.sanidadbackend.inteligencia.recommendations.RecommendationService;
import com.farmacia.sanidadbackend.model.Usuario;
import com.farmacia.sanidadbackend.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssistantService {

    private final VentaRepository ventaRepository;
    private final LoteDetalleRepository loteDetalleRepository;
    private final LoteRepository loteRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final PerdidasService perdidasService;
    private final RecommendationService recommendationService;
    private final VentaDetalleRepository ventaDetalleRepository;

    @Autowired
    private AiAssistantService aiAssistantService;

    private List<Intent> intents;

    @PostConstruct
    public void init() {
        intents = new ArrayList<>();

        intents.add(new Intent(
                List.of("ventas", "dia"),
                List.of("hoy", "ingresos"),
                false,
                u -> obtenerVentasDelDia()
        ));

        intents.add(new Intent(
                List.of("bajo", "stock"),
                List.of("poco", "escaso"),
                false,
                u -> obtenerProductosBajoStock()
        ));

        intents.add(new Intent(
                List.of("recomendaciones"),
                List.of("sugerencias"),
                true,
                u -> obtenerRecomendaciones()
        ));
    }

    private record Intent(List<String> keywords, List<String> synonyms, boolean onlyAdmin,
                          Function<Usuario, AssistantResponse> action) {}

    private String normalizar(String texto) {
        return texto.toLowerCase();
    }

    private int calcularScore(Intent intent, String query) {
        int score = 0;
        for (String kw : intent.keywords) {
            if (query.contains(kw)) score += 3;
        }
        for (String syn : intent.synonyms) {
            if (query.contains(syn)) score += 1;
        }
        return score;
    }

    // 🔥 MÉTODO PRINCIPAL (SIEMPRE IA) - TRANSACCIÓN SIN readOnly
    @Transactional
    public AssistantResponse procesarConsulta(String query, Usuario usuario) {

        String queryOriginal = query;
        query = normalizar(query);

        boolean isAdmin = "ADMIN".equals(usuario.getRol());

        Intent bestIntent = null;
        int bestScore = 0;

        for (Intent intent : intents) {
            if (intent.onlyAdmin && !isAdmin) continue;

            int score = calcularScore(intent, query);
            if (score > bestScore) {
                bestScore = score;
                bestIntent = intent;
            }
        }

        StringBuilder contexto = new StringBuilder();

        // 🔥 Ejecuta lógica interna SOLO como contexto
        if (bestIntent != null && bestScore >= 2) {
            AssistantResponse intentResponse = bestIntent.action.apply(usuario);

            contexto.append("DATOS DEL SISTEMA:\n");
            contexto.append(intentResponse.getAnswer()).append("\n\n");
        }

        // 🔥 SIEMPRE IA
        String aiAnswer = aiAssistantService.consultarIAConContexto(
                queryOriginal,
                usuario,
                contexto.toString()
        );

        return AssistantResponse.builder()
                .answer(aiAnswer)
                .build();
    }

    // ================== MÉTODOS SIMPLES ==================

    private AssistantResponse obtenerVentasDelDia() {
        return AssistantResponse.builder()
                .answer("Ventas del día: C$ 1500")
                .build();
    }

    private AssistantResponse obtenerProductosBajoStock() {
        return AssistantResponse.builder()
                .answer("Paracetamol - 5 unidades\nIbuprofeno - 3 unidades")
                .build();
    }

    private AssistantResponse obtenerRecomendaciones() {
        recommendationService.generarRecomendaciones();

        List<Recommendation> lista = recommendationService.obtenerRecomendacionesPendientes();

        if (lista.isEmpty()) {
            return AssistantResponse.builder().answer("No hay recomendaciones").build();
        }

        StringBuilder sb = new StringBuilder();

        Map<RecommendationPriority, List<Recommendation>> agrupadas =
                lista.stream().collect(Collectors.groupingBy(Recommendation::getPriority));

        for (RecommendationPriority p : RecommendationPriority.values()) {
            if (!agrupadas.containsKey(p)) continue;

            sb.append(p.name()).append(":\n");

            for (Recommendation r : agrupadas.get(p)) {
                sb.append("- ").append(r.getTitle()).append("\n");
            }
        }

        return AssistantResponse.builder().answer(sb.toString()).build();
    }
}