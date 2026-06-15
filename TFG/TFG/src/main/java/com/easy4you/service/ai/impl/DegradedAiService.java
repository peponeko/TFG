package com.easy4you.service.ai.impl;

import com.easy4you.service.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(AiService.class)
// Implementación mínima cuando no hay proveedor configurado.
public class DegradedAiService implements AiService {

  private static final Logger log = LoggerFactory.getLogger(DegradedAiService.class);

  @Override
  public String generarRespuesta(String prompt, int maxTokens) {
    log.debug("DegradedAiService activo: proveedor no disponible");
    return "";
  }

  @Override
  public String generarConContexto(String sistemaPrompt, String contexto, String pregunta, int maxTokens) {
    log.debug("DegradedAiService activo: proveedor no disponible");
    return "";
  }

  @Override
  public String generarJson(String prompt) {
    log.debug("DegradedAiService activo: proveedor no disponible");
    return "[]";
  }

  @Override
  public boolean isDisponible() {
    return false;
  }
}
