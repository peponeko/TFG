package com.easy4you.service.ai.impl;

import com.easy4you.config.AiProperties;
import com.easy4you.exception.ServiceUnavailableException;
import com.easy4you.service.ai.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaAiService implements AiService {

  private static final Logger log = LoggerFactory.getLogger(OllamaAiService.class);

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);
  private static final Duration HEALTHCHECK_TIMEOUT = Duration.ofSeconds(1);

  private static final String MSG_IA_NO_DISPONIBLE =
      "La IA local no está disponible. Ejecuta: ollama run tinyllama";
  private static final String MSG_IA_RESPUESTA_INVALIDA =
      "La IA no devolvió una respuesta válida. Inténtalo de nuevo.";

  private final AiProperties aiProperties;
  private final ObjectMapper objectMapper;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

  @Override
  public String generarRespuesta(String prompt, int maxTokens) {
    if (prompt == null || prompt.isBlank()) {
      return "";
    }

    String url = buildUrl("/api/generate");
    String modelName = aiProperties.getOllama().getModel();

    log.info("Llamando a Ollama con modelo: {}, maxTokens: {}", modelName, maxTokens);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", modelName);
    body.put("prompt", prompt);
    body.put("stream", false);
    body.put("keep_alive", "5m");  // Mantener modelo en memoria 5 minutos
    body.put(
        "options",
        Map.of(
            "num_predict", Math.max(1, maxTokens),
            "temperature", 0.2,
            "top_p", 0.9,
            "repeat_penalty", 1.1));

    try {
      String json = objectMapper.writeValueAsString(body);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(REQUEST_TIMEOUT)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      
      log.debug("Ollama response status: {}, body length: {}", response.statusCode(), response.body().length());
      
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.error("Ollama error status={}: {}", response.statusCode(), safeBody(response.body()));
        throw new ServiceUnavailableException(
            "Error llamando a la IA (Ollama): HTTP " + response.statusCode() + " - " + safeBody(response.body()));
      }

      String raw = response.body();
      if (raw == null || raw.isBlank()) {
        log.error("Ollama devolvió body vacío (status={})", response.statusCode());
        throw new ServiceUnavailableException(MSG_IA_RESPUESTA_INVALIDA);
      }

      OllamaGenerateResponse parsed;
      try {
        parsed = objectMapper.readValue(raw, OllamaGenerateResponse.class);
      } catch (Exception ex) {
        log.error("Ollama devolvió JSON inválido: {}", safeBody(raw), ex);
        throw new ServiceUnavailableException(MSG_IA_RESPUESTA_INVALIDA);
      }

      String result = parsed == null ? null : parsed.response();
      if (result == null || result.isBlank()) {
        log.error("Ollama devolvió JSON sin 'response' (o vacío): {}", safeBody(raw));
        throw new ServiceUnavailableException(MSG_IA_RESPUESTA_INVALIDA);
      }

      String trimmed = result.trim();
      log.info("Ollama generación completada, longitud: {}", trimmed.length());
      return trimmed;
    } catch (ServiceUnavailableException ex) {
      throw ex;
    } catch (HttpTimeoutException | ConnectException ex) {
      log.error("Ollama no responde (timeout/connection refused): {}", ex.getMessage(), ex);
      throw new ServiceUnavailableException(MSG_IA_NO_DISPONIBLE);
    } catch (Exception ex) {
      if (isConnectionRefusedOrTimeout(ex)) {
        log.error("Ollama no responde (timeout/connection refused): {}", ex.getMessage(), ex);
        throw new ServiceUnavailableException(MSG_IA_NO_DISPONIBLE);
      }
      log.error("Excepción llamando a Ollama: {}", ex.getMessage(), ex);
      throw new ServiceUnavailableException("No se ha podido llamar a la IA (Ollama): " + ex.getMessage());
    }
  }

  @Override
  public String generarConContexto(String sistemaPrompt, String contexto, String pregunta, int maxTokens) {
    StringBuilder sb = new StringBuilder();
    if (sistemaPrompt != null && !sistemaPrompt.isBlank()) {
      sb.append(sistemaPrompt.trim()).append("\n\n");
    }
    if (contexto != null && !contexto.isBlank()) {
      sb.append(contexto.trim()).append("\n\n");
    }
    if (pregunta != null && !pregunta.isBlank()) {
      sb.append(pregunta.trim());
    }
    return generarRespuesta(sb.toString(), maxTokens);
  }

  @Override
  public boolean isDisponible() {
    String url = buildUrl("/api/tags");
    try {
      HttpRequest request =
          HttpRequest.newBuilder().uri(URI.create(url)).timeout(HEALTHCHECK_TIMEOUT).GET().build();
      HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      return response.statusCode() >= 200 && response.statusCode() < 300;
    } catch (Exception ex) {
      return false;
    }
  }

  private String buildUrl(String path) {
    String baseUrl = aiProperties.getOllama().getBaseUrl();
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = "http://localhost:11434";
    }
    String b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    String p = path.startsWith("/") ? path : "/" + path;
    return b + p;
  }

  private String safeBody(String body) {
    if (body == null) {
      return "";
    }
    String trimmed = body.trim();
    if (trimmed.length() <= 250) {
      return trimmed;
    }
    return trimmed.substring(0, 240) + "…";
  }

  private boolean isConnectionRefusedOrTimeout(Throwable ex) {
    Throwable t = ex;
    while (t != null) {
      if (t instanceof HttpConnectTimeoutException
          || t instanceof HttpTimeoutException
          || t instanceof ConnectException) {
        return true;
      }
      t = t.getCause();
    }
    return false;
  }

  private record OllamaGenerateResponse(String response) {}
}
