package com.easy4you.service.ai;

public interface AiService {
  String generarRespuesta(String prompt, int maxTokens);

  String generarConContexto(String sistemaPrompt, String contexto, String pregunta, int maxTokens);

  boolean isDisponible();

  /**
   * Genera contenido en formato JSON puro.
   * Gemini usa JSON Mode (response_mime_type: application/json).
   * Ollama/Degraded delegan en generarRespuesta con maxTokens=4096.
   */
  default String generarJson(String prompt) {
    return generarRespuesta(prompt, 4096);
  }
}
