package com.easy4you.service.ai.impl;

import com.easy4you.service.ai.AiService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(AiService.class)
public class DegradedAiService implements AiService {

  @Override
  public String generarRespuesta(String prompt, int maxTokens) {
    throw new UnsupportedOperationException("Proveedor de IA no configurado");
  }

  @Override
  public String generarConContexto(String sistemaPrompt, String contexto, String pregunta, int maxTokens) {
    throw new UnsupportedOperationException("Proveedor de IA no configurado");
  }

  @Override
  public boolean isDisponible() {
    return false;
  }
}

