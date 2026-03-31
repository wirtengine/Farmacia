package com.farmacia.sanidadbackend.inteligencia.assistant;

import com.farmacia.sanidadbackend.inteligencia.assistant.dto.AssistantRequest;
import com.farmacia.sanidadbackend.inteligencia.assistant.dto.AssistantResponse;
import com.farmacia.sanidadbackend.model.Usuario;
import com.farmacia.sanidadbackend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/ask")
    public ResponseEntity<AssistantResponse> ask(
            @RequestBody AssistantRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if (userDetails == null || userDetails.getUsuario() == null) {
            return ResponseEntity.status(401).body(
                    AssistantResponse.builder()
                            .answer("Usuario no autenticado.")
                            .build()
            );
        }

        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest().body(
                    AssistantResponse.builder()
                            .answer("La consulta no puede estar vacía.")
                            .build()
            );
        }

        Usuario usuario = userDetails.getUsuario();
        AssistantResponse response = assistantService.procesarConsulta(
                request.getQuery(),
                usuario
        );

        return ResponseEntity.ok(response);
    }
}