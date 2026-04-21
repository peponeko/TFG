package com.easy4you.service.ai;

public interface AiService {
  String generarRespuesta(String prompt, int maxTokens);

  String generarConContexto(String sistemaPrompt, String contexto, String pregunta, int maxTokens);

  boolean isDisponible();
}

