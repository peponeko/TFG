package com.easy4you.service.ai.impl;

import com.easy4you.service.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(AiService.class)
// Servicio de IA de emergencia: devuelve respuestas vacías si Ollama/Gemini no están disponibles
public class DegradedAiService implements AiService {

  private static final Logger log = LoggerFactory.getLogger(DegradedAiService.class);

  @Override
  public String generarRespuesta(String prompt, int maxTokens) {
    log.info("DegradedAiService activo: proveedor de IA no disponible");
    return "";
  }

  @Override
  public String generarConContexto(String sistemaPrompt, String contexto, String pregunta, int maxTokens) {
    log.info("DegradedAiService activo: proveedor de IA no disponible");
    return "";
  }

  @Override
  public String generarJson(String prompt) {
    log.info("DegradedAiService activo: proveedor de IA no disponible");
    return "[]";
  }

  @Override
  public boolean isDisponible() {
    return false;
  }
}
