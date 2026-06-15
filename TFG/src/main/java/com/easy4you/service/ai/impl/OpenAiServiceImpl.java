package com.easy4you.service.ai.impl;

import com.easy4you.config.AiProperties;
import com.easy4you.service.ai.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiServiceImpl implements AiService {

  private static final Logger log = LoggerFactory.getLogger(OpenAiServiceImpl.class);

  private static final String OPENAI_CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

  private static final int DEFAULT_MAX_TOKENS = 800;
  private static final String FALLBACK_CHAT =
      "Ahora mismo la IA no está disponible. Para la demo, puedo ayudarte igualmente: revisa el texto extraído del documento y formula una pregunta más concreta.";

  private final AiProperties aiProperties;
  private final ObjectMapper objectMapper;

  private final RestTemplate restTemplate = new RestTemplate();

  @Override
  public String generarRespuesta(String prompt, int maxTokens) {
    if (prompt == null || prompt.isBlank()) {
      return "";
    }
    try {
      return callChatCompletions(
          "Eres un asistente académico. Responde en español. No inventes información.",
          prompt,
          Math.max(64, maxTokens));
    } catch (Exception ex) {
      log.warn("OpenAI no disponible, activando fallback: {}", ex.getMessage());
      return FALLBACK_CHAT;
    }
  }

  @Override
  public String generarConContexto(String sistemaPrompt, String contexto, String pregunta, int maxTokens) {
    String system = (sistemaPrompt == null || sistemaPrompt.isBlank()) ? "Responde en español. No inventes." : sistemaPrompt;
    StringBuilder user = new StringBuilder();
    if (contexto != null && !contexto.isBlank()) {
      user.append("CONTEXTO:\n").append(contexto.trim()).append("\n\n");
    }
    if (pregunta != null && !pregunta.isBlank()) {
      user.append("PREGUNTA:\n").append(pregunta.trim());
    }
    if (user.isEmpty()) {
      return "";
    }
    try {
      return callChatCompletions(system, user.toString(), Math.max(64, maxTokens));
    } catch (Exception ex) {
      log.warn("OpenAI no disponible, activando fallback: {}", ex.getMessage());
      return FALLBACK_CHAT;
    }
  }

  @Override
  public String generarJson(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      return "[]";
    }
    try {
      String out =
          callChatCompletions(
              "Devuelve exclusivamente JSON válido (sin markdown, sin texto adicional). Responde en español si hay campos de texto.",
              prompt,
              4096);
      // Si devolviera basura, al menos no rompemos el frontend: fallback JSON.
      if (out == null) {
        return "[]";
      }
      String trimmed = out.trim();
      if (trimmed.startsWith("```")) {
        return "[]";
      }
      return trimmed;
    } catch (Exception ex) {
      log.warn("OpenAI no disponible (JSON), activando fallback: {}", ex.getMessage());
      return "[]";
    }
  }

  @Override
  public boolean isDisponible() {
    String key = resolveApiKey();
    return key != null && !key.isBlank();
  }

  private String callChatCompletions(String system, String user, int maxTokens) throws Exception {
    String apiKey = resolveApiKey();
    String model = aiProperties.getOpenai().getModel();

    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("OpenAI API key no configurada (ai.openai.api-key)");
    }
    if (model == null || model.isBlank()) {
      model = "gpt-4o-mini";
    }

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("messages", List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)));
    body.put("temperature", 0.2);
    body.put("max_tokens", Math.max(64, maxTokens > 0 ? maxTokens : DEFAULT_MAX_TOKENS));

    String json = objectMapper.writeValueAsString(body);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    // Timeout simple a nivel HTTP client (RestTemplate por defecto usa timeouts del JDK/HttpUrlConnection);
    // mantenemos la implementación simple para DAM.
    HttpEntity<String> entity = new HttpEntity<>(json, headers);

    ResponseEntity<String> response;
    try {
      response = restTemplate.postForEntity(OPENAI_CHAT_COMPLETIONS_URL, entity, String.class);
    } catch (RestClientException ex) {
      throw new RestClientException("Error llamando a OpenAI: " + ex.getMessage(), ex);
    }

    String responseBody = response.getBody();
    if (responseBody == null || responseBody.isBlank()) {
      throw new IllegalStateException("OpenAI devolvió una respuesta vacía");
    }

    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode content = root.path("choices").path(0).path("message").path("content");
    if (content.isMissingNode() || content.isNull()) {
      throw new IllegalStateException("OpenAI no devolvió contenido utilizable");
    }
    String out = content.asText("");
    return out == null ? "" : out.trim();
  }

  private String resolveApiKey() {
    String key = aiProperties.getOpenai().getApiKey();
    if (key != null && !key.isBlank()) {
      return key;
    }
    // Fallback para demos en Windows/IDE: a veces la variable de entorno no llega al proceso.
    String env = System.getenv("OPENAI_API_KEY");
    return env == null ? "" : env.trim();
  }
}

