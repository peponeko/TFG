package com.easy4you.service.impl;

import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.exception.ServiceUnavailableException;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.DocumentoChunk;
import com.easy4you.model.entity.PreguntaTest;
import com.easy4you.model.entity.PreguntaTestOpcion;
import com.easy4you.model.entity.Usuario;
import com.easy4you.model.enums.EstadoProcesadoDocumento;
import com.easy4you.repository.DocumentoChunkRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.service.PreguntaTestGenerationService;
import com.easy4you.service.ai.AiService;
import com.easy4you.util.PromptTemplates;
import com.easy4you.util.TextUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PreguntaTestGenerationServiceImpl implements PreguntaTestGenerationService {

  private static final Logger log = LoggerFactory.getLogger(PreguntaTestGenerationServiceImpl.class);

  private static final String MSG_DOC_NO_PROCESADO =
      "El documento aún no está procesado. Espera unos segundos y recarga la página.";

  private static final int NUM_PREGUNTAS = 8;
  // Gemini 1.5 Flash soporta hasta 1M tokens: enviamos hasta 100k chars de texto
  private static final int MAX_INPUT_CHARS = 100_000;
  private static final int KEYWORD_MAX = 6;
  private static final int CHUNKS_PER_KEYWORD = 20;

  private static final Set<String> STOPWORDS =
      Set.of(
          "a",
          "al",
          "algo",
          "algunos",
          "ante",
          "antes",
          "como",
          "con",
          "contra",
          "cual",
          "cuando",
          "de",
          "del",
          "desde",
          "donde",
          "durante",
          "e",
          "el",
          "ella",
          "ellas",
          "ellos",
          "en",
          "entre",
          "es",
          "esa",
          "ese",
          "eso",
          "esta",
          "este",
          "esto",
          "estos",
          "fue",
          "ha",
          "hay",
          "la",
          "las",
          "lo",
          "los",
          "mas",
          "más",
          "muy",
          "no",
          "o",
          "otra",
          "otro",
          "para",
          "pero",
          "por",
          "porque",
          "que",
          "quien",
          "se",
          "si",
          "sin",
          "sobre",
          "su",
          "sus",
          "tambien",
          "también",
          "todo",
          "todos",
          "un",
          "una",
          "y");

  private final UsuarioRepository usuarioRepository;
  private final DocumentoRepository documentoRepository;
  private final DocumentoChunkRepository documentoChunkRepository;
  private final PreguntaTestRepository preguntaTestRepository;
  private final AiService aiService;
  private final ObjectMapper objectMapper;
  private final TaskExecutor taskExecutor;

  @Override
  public List<PreguntaTest> generarParaDocumento(Long usuarioId, Long documentoId) {
    return generarParaDocumentoInternal(usuarioId, documentoId, true);
  }

  @Override
  public void solicitarGeneracionParaDocumento(Long usuarioId, Long documentoId) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (documentoId == null) {
      throw new BadRequestException("documentoId es obligatorio");
    }

    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(documentoId, usuarioId)
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + documentoId));

    if (!isDocumentoListo(documento)) {
      throw new BadRequestException(MSG_DOC_NO_PROCESADO);
    }
    if (documento.getTextoExtraido() == null || documento.getTextoExtraido().isBlank()) {
      throw new BadRequestException("El documento no tiene texto extraído");
    }
    if (documento.getTema() == null) {
      throw new BadRequestException("El documento no tiene tema. Asigna un tema antes de generar tests.");
    }

    documento.setEstadoProcesado(EstadoProcesadoDocumento.PROCESANDO);
    documento.setErrorExtraccion(null);
    documentoRepository.save(documento);

    taskExecutor.execute(() -> generarParaDocumentoAsync(usuarioId, documentoId));
  }

  @Async
  @Transactional
  @Override
  public void generarParaDocumentoAsync(Long usuarioId, Long documentoId) {
    try {
      generarParaDocumentoInternal(usuarioId, documentoId, false);
      updateDocumentoEstado(usuarioId, documentoId, EstadoProcesadoDocumento.LISTO, null);
    } catch (Exception ex) {
      log.error("Error generando preguntas test: documentoId={}, usuarioId={}", documentoId, usuarioId, ex);
      updateDocumentoEstado(usuarioId, documentoId, EstadoProcesadoDocumento.PROCESADO, null);
    }
  }

  private void updateDocumentoEstado(
      Long usuarioId, Long documentoId, EstadoProcesadoDocumento estado, String error) {
    if (usuarioId == null || documentoId == null) {
      return;
    }
    documentoRepository
        .findByIdAndUsuarioId(documentoId, usuarioId)
        .ifPresent(
            doc -> {
              doc.setEstadoProcesado(estado);
              doc.setErrorExtraccion(error);
              documentoRepository.save(doc);
            });
  }

  private List<PreguntaTest> generarParaDocumentoInternal(Long usuarioId, Long documentoId, boolean validarEstado) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (documentoId == null) {
      throw new BadRequestException("documentoId es obligatorio");
    }

    Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + usuarioId));

    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(documentoId, usuarioId)
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + documentoId));

    if (validarEstado && !isDocumentoListo(documento)) {
      throw new BadRequestException(MSG_DOC_NO_PROCESADO);
    }

    if (documento.getTextoExtraido() == null || documento.getTextoExtraido().isBlank()) {
      throw new BadRequestException("El documento no tiene texto extraído");
    }
    if (documento.getTema() == null) {
      throw new BadRequestException("El documento no tiene tema. Asigna un tema antes de generar tests.");
    }

    String input = truncate(documento.getTextoExtraido(), MAX_INPUT_CHARS);
    String prompt = PromptTemplates.formatPreguntasTest(input, NUM_PREGUNTAS);
    // Gemini JSON Mode → devuelve JSON puro, sin necesidad de JsonExtractor
    String jsonArray = aiService.generarJson(prompt);
    if (jsonArray == null || jsonArray.isBlank() || "[]".equals(jsonArray.trim())) {
      throw new ServiceUnavailableException(
          "La IA no devolvió preguntas válidas. Comprueba OPENAI_API_KEY y el modelo configurado.");
    }

    List<PreguntaGenerated> generated = parsePreguntas(jsonArray);
    if (generated.isEmpty()) {
      throw new ServiceUnavailableException("La IA devolvió 0 preguntas");
    }

    List<PreguntaTest> toSave = new ArrayList<>();
    for (PreguntaGenerated g : generated) {
      String pregunta = safeTrim(g.pregunta());
      if (pregunta.isBlank()) {
        continue;
      }

      List<String> opciones = g.opciones() == null ? List.of() : g.opciones().stream().filter(Objects::nonNull).toList();
      if (opciones.size() < 4) {
        continue;
      }
      List<String> cuatro = opciones.size() > 4 ? opciones.subList(0, 4) : opciones;

      int indiceCorrecto = parseIndiceCorrecto(g.respuestaCorrecta());
      if (indiceCorrecto < 0 || indiceCorrecto > 3) {
        indiceCorrecto = 0;
      }

      PreguntaTest pt = new PreguntaTest();
      pt.setUsuario(usuario);
      pt.setTema(documento.getTema());
      pt.setDocumento(documento);
      pt.setChunkOrigen(resolveChunkOrigen(documentoId, buildChunkOriginText(g)));
      pt.setEnunciado(pregunta);
      pt.setExplicacion(safeTrim(g.explicacion()));
      pt.setDificultad(3);

      for (int i = 0; i < 4; i++) {
        String texto = safeTrim(cuatro.get(i));
        if (texto.isBlank()) {
          texto = "(sin texto)";
        }
        texto = trimToMax(texto, 400);

        PreguntaTestOpcion op = new PreguntaTestOpcion();
        op.setPreguntaTest(pt);
        op.setOrden(i);
        op.setTexto(texto);
        op.setEsCorrecta(i == indiceCorrecto);
        pt.getOpciones().add(op);
      }

      toSave.add(pt);
    }

    if (toSave.isEmpty()) {
      throw new ServiceUnavailableException("No se pudieron construir preguntas válidas");
    }

    List<PreguntaTest> saved = preguntaTestRepository.saveAll(toSave);
    log.info("Preguntas test generadas: documentoId={}, usuarioId={}, count={}", documentoId, usuarioId, saved.size());
    return saved;
  }

  private boolean isDocumentoListo(Documento documento) {
    EstadoProcesadoDocumento estado = documento.getEstadoProcesado();
    return estado == EstadoProcesadoDocumento.PROCESADO || estado == EstadoProcesadoDocumento.LISTO;
  }

  private List<PreguntaGenerated> parsePreguntas(String jsonArray) {
    try {
      List<PreguntaGenerated> parsed =
          objectMapper.readValue(jsonArray, new TypeReference<List<PreguntaGenerated>>() {});
      return parsed == null ? List.of() : parsed;
    } catch (Exception ex) {
      return List.of();
    }
  }

  private String buildChunkOriginText(PreguntaGenerated g) {
    StringBuilder sb = new StringBuilder();
    sb.append(safeTrim(g.pregunta())).append(' ');
    sb.append(safeTrim(g.explicacion())).append(' ');
    if (g.opciones() != null) {
      for (String op : g.opciones()) {
        sb.append(safeTrim(op)).append(' ');
      }
    }
    return sb.toString().trim();
  }

  private int parseIndiceCorrecto(String respuestaCorrecta) {
    if (respuestaCorrecta == null) {
      return -1;
    }
    String t = respuestaCorrecta.trim().toLowerCase(Locale.ROOT);
    if (t.isBlank()) {
      return -1;
    }
    char c = t.charAt(0);
    return switch (c) {
      case 'a' -> 0;
      case 'b' -> 1;
      case 'c' -> 2;
      case 'd' -> 3;
      default -> -1;
    };
  }

  private DocumentoChunk resolveChunkOrigen(Long documentoId, String texto) {
    List<String> keywords = extractKeywords(texto);
    Map<Long, ScoredChunk> candidates = new HashMap<>();

    for (String kw : keywords) {
      Page<DocumentoChunk> page =
          documentoChunkRepository.findByDocumentoIdInAndTextoContaining(
              List.of(documentoId), kw, PageRequest.of(0, CHUNKS_PER_KEYWORD));
      for (DocumentoChunk chunk : page.getContent()) {
        if (chunk == null || chunk.getId() == null) {
          continue;
        }
        String haystack = normalizeForScoring(chunk.getTexto());
        int occ = countOccurrences(haystack, kw);
        ScoredChunk sc = candidates.computeIfAbsent(chunk.getId(), id -> new ScoredChunk(chunk));
        sc.matchedKeywords.add(kw);
        sc.occurrences += Math.max(1, occ);
      }
    }

    if (!candidates.isEmpty()) {
      return candidates.values().stream()
          .sorted((a, b) -> Integer.compare(b.score(), a.score()))
          .map(sc -> sc.chunk)
          .findFirst()
          .orElse(null);
    }

    Page<DocumentoChunk> fallback =
        documentoChunkRepository.findByDocumentoIdOrderByIndiceChunkAsc(documentoId, PageRequest.of(0, 1));
    return fallback.getContent().isEmpty() ? null : fallback.getContent().get(0);
  }

  private List<String> extractKeywords(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }

    String normalized = normalizeForScoring(text);
    String[] raw = normalized.split("\\s+");

    LinkedHashSet<String> keywords = new LinkedHashSet<>();
    for (String token : raw) {
      String t = token.trim();
      if (t.isBlank()) {
        continue;
      }
      if (t.length() < 4) {
        continue;
      }
      if (STOPWORDS.contains(t)) {
        continue;
      }
      keywords.add(t);
      if (keywords.size() >= KEYWORD_MAX) {
        break;
      }
    }

    return List.copyOf(keywords);
  }

  private String normalizeForScoring(String text) {
    if (text == null) {
      return "";
    }
    String n = Normalizer.normalize(text, Normalizer.Form.NFD);
    n = n.replaceAll("\\p{M}", "");
    n = n.toLowerCase(Locale.ROOT);
    n = n.replaceAll("[^\\p{L}\\p{N}\\s]", " ");
    return n.replaceAll("\\s+", " ").trim();
  }

  private int countOccurrences(String haystack, String needle) {
    if (haystack == null || needle == null || haystack.isBlank() || needle.isBlank()) {
      return 0;
    }
    int count = 0;
    int idx = 0;
    while (true) {
      int found = haystack.indexOf(needle, idx);
      if (found < 0) {
        break;
      }
      count++;
      idx = found + needle.length();
    }
    return count;
  }

  private String truncate(String text, int maxChars) {
    return TextUtils.truncate(text, maxChars);
  }

  private String safeTrim(String s) {
    return TextUtils.safeTrim(s);
  }

  private String trimToMax(String s, int max) {
    return TextUtils.truncate(s, max);
  }

  private record PreguntaGenerated(
      String pregunta, List<String> opciones, String respuestaCorrecta, String explicacion) {}

  private static class ScoredChunk {
    private final DocumentoChunk chunk;
    private final Set<String> matchedKeywords = new LinkedHashSet<>();
    private int occurrences = 0;

    private ScoredChunk(DocumentoChunk chunk) {
      this.chunk = chunk;
    }

    private int score() {
      return matchedKeywords.size() * 10 + occurrences;
    }
  }
}
