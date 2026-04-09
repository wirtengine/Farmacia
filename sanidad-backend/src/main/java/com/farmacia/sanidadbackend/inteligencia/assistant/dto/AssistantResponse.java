package com.farmacia.sanidadbackend.inteligencia.assistant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssistantResponse {
    private String answer;
}