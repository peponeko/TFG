package com.easy4you.service.ai.impl;

import com.easy4you.config.AiProperties;
import com.easy4you.exception.ServiceUnavailableException;
import com.easy4you.service.ai.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
public class GeminiAiServiceImpl implements AiService {

  private static final Logger log = LoggerFactory.getLogger(GeminiAiServiceImpl.class);

  private static final String GEMINI_BASE_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/";

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
  private static final Duration REQUEST_TIMEOUT  = Duration.ofSeconds(120);

  private final AiProperties aiProperties;
  private final ObjectMapper objectMapper;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

  // -------------------------------------------------------------------------
  // AiService contract
  // -------------------------------------------------------------------------

  @Override
  public String generarRespuesta(String prompt, int maxTokens) {
    if (prompt == null || prompt.isBlank()) {
      return "";
    }
    log.info("Gemini generarRespuesta: promptLen={}, maxTokens={}", prompt.length(), maxTokens);
    return llamarGemini(prompt, false, maxTokens);
  }

  @Override
  public String generarConContexto(String sistemaPrompt, String contexto,
                                    String pregunta, int maxTokens) {
    StringBuilder sb = new StringBuilder();
    if (sistemaPrompt != null && !sistemaPrompt.isBlank()) {
      sb.append(sistemaPrompt.trim()).append("\n\n");
    }
    if (contexto != null && !contexto.isBlank()) {
      sb.append("CONTEXTO:\n").append(contexto.trim()).append("\n\n");
    }
    if (pregunta != null && !pregunta.isBlank()) {
      sb.append("PREGUNTA:\n").append(pregunta.trim());
    }
    return generarRespuesta(sb.toString(), maxTokens);
  }

  // JSON Mode: Gemini devolverá JSON puro sin markdown ni texto adicional.
  @Override
  public String generarJson(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      return "[]";
    }
    log.info("Gemini generarJson (JSON Mode): promptLen={}", prompt.length());
    return llamarGemini(prompt, true, 4096);
  }

  @Override
  public boolean isDisponible() {
    // Gemini es un servicio cloud; disponible si la API Key está configurada
    String key = apiKey();
    return key != null && !key.isBlank();
  }

  // -------------------------------------------------------------------------
  // Llamada HTTP a la API de Gemini
  // -------------------------------------------------------------------------

  private String llamarGemini(String prompt, boolean jsonMode, int maxTokens) {
    String model  = aiProperties.getGemini().getModel();
    String apiKey = apiKey();
    String url    = GEMINI_BASE_URL + model + ":generateContent?key=" + apiKey;

    // --- Construcción del body ---
    Map<String, Object> part     = Map.of("text", prompt);
    Map<String, Object> content  = Map.of("parts", List.of(part));

    Map<String, Object> generationConfig = new LinkedHashMap<>();
    generationConfig.put("maxOutputTokens", Math.max(256, maxTokens));
    generationConfig.put("temperature", jsonMode ? 0.1 : 0.7);
    if (jsonMode) {
      generationConfig.put("response_mime_type", "application/json");
    }

    Map<String, Object> requestBody = new LinkedHashMap<>();
    requestBody.put("contents", List.of(content));
    requestBody.put("generationConfig", generationConfig);

    try {
      String bodyJson = objectMapper.writeValueAsString(requestBody);
      log.debug("Gemini request → model={}, jsonMode={}, bodyLen={}", model, jsonMode, bodyJson.length());

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(REQUEST_TIMEOUT)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
          .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      log.debug("Gemini response: status={}, bodyLen={}", response.statusCode(), response.body().length());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        String shortBody = safeBody(response.body());
        log.error("Gemini error HTTP {}: {}", response.statusCode(), shortBody);
        throw new ServiceUnavailableException(
            "Error en la API de Gemini (HTTP " + response.statusCode() + "): " + shortBody);
      }

      return extractTextFromResponse(response.body());

    } catch (ServiceUnavailableException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Excepción llamando a Gemini: {}", ex.getMessage(), ex);
      throw new ServiceUnavailableException("No se pudo conectar con Gemini: " + ex.getMessage());
    }
  }

  // Extrae el texto del JSON de respuesta de Gemini: candidates[0].content.parts[0].text
  private String extractTextFromResponse(String body) {
    try {
      JsonNode root       = objectMapper.readTree(body);
      JsonNode candidates = root.path("candidates");
      if (!candidates.isArray() || candidates.isEmpty()) {
        log.error("Gemini: sin candidates en la respuesta. Body={}", safeBody(body));
        throw new ServiceUnavailableException("Gemini no devolvió candidatos en la respuesta.");
      }
      JsonNode text = candidates.get(0)
          .path("content")
          .path("parts")
          .get(0)
          .path("text");

      if (text == null || text.isMissingNode() || text.isNull()) {
        // Puede haber finishReason=SAFETY o similar
        JsonNode finishReason = candidates.get(0).path("finishReason");
        log.error("Gemini: texto vacío, finishReason={}", finishReason.asText("desconocido"));
        throw new ServiceUnavailableException(
            "Gemini no devolvió texto. Motivo: " + finishReason.asText("desconocido"));
      }

      String result = text.asText().trim();
      log.info("Gemini OK: responseLen={}", result.length());
      return result;

    } catch (ServiceUnavailableException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Error parseando respuesta de Gemini: {}", safeBody(body), ex);
      throw new ServiceUnavailableException("Error procesando la respuesta de Gemini.");
    }
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private String apiKey() {
    return aiProperties.getGemini().getApiKey();
  }

  private String safeBody(String body) {
    if (body == null) return "";
    String t = body.trim();
    return t.length() <= 300 ? t : t.substring(0, 290) + "…";
  }
}
