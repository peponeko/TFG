package com.easy4you.service.impl;

import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.exception.ServiceUnavailableException;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.Flashcard;
import com.easy4you.model.entity.Usuario;
import com.easy4you.model.enums.EstadoProcesadoDocumento;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.service.FlashcardGenerationService;
import com.easy4you.service.ai.AiService;
import com.easy4you.util.PromptTemplates;
import com.easy4you.util.TextUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlashcardGenerationServiceImpl implements FlashcardGenerationService {

  private static final Logger log = LoggerFactory.getLogger(FlashcardGenerationServiceImpl.class);

  private static final String MSG_DOC_NO_PROCESADO =
      "El documento aún no está procesado. Espera unos segundos y recarga la página.";

  private static final int NUM_FLASHCARDS = 10;
  private static final int MAX_INPUT_CHARS = 100_000;

  private final UsuarioRepository usuarioRepository;
  private final DocumentoRepository documentoRepository;
  private final FlashcardRepository flashcardRepository;
  private final AiService aiService;
  private final ObjectMapper objectMapper;
  private final TaskExecutor taskExecutor;

  @Override
  public List<Flashcard> generarParaDocumento(Long usuarioId, Long documentoId) {
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
      throw new BadRequestException("El documento no tiene tema. Asigna un tema antes de generar flashcards.");
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
      log.error("Error generando flashcards: documentoId={}, usuarioId={}", documentoId, usuarioId, ex);
      // No marcar el documento como ERROR de extracción: solo falló la generación con IA.
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

  private List<Flashcard> generarParaDocumentoInternal(Long usuarioId, Long documentoId, boolean validarEstado) {
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
      throw new BadRequestException("El documento no tiene tema. Asigna un tema antes de generar flashcards.");
    }

    String input = truncate(documento.getTextoExtraido(), MAX_INPUT_CHARS);
    String prompt = PromptTemplates.formatFlashcards(input, NUM_FLASHCARDS);
    // Gemini JSON Mode → devuelve JSON puro, sin necesidad de JsonExtractor
    String jsonArray = aiService.generarJson(prompt);
    if (jsonArray == null || jsonArray.isBlank() || "[]".equals(jsonArray.trim())) {
      throw new ServiceUnavailableException(
          "La IA no devolvió flashcards válidas. Comprueba OPENAI_API_KEY y el modelo configurado.");
    }

    List<FlashcardGenerated> generated = parseFlashcards(jsonArray);
    if (generated.isEmpty()) {
      throw new ServiceUnavailableException("La IA devolvió 0 flashcards");
    }

    List<Flashcard> toSave = new ArrayList<>();
    for (FlashcardGenerated g : generated) {
      if (g.pregunta() == null || g.pregunta().isBlank() || g.respuesta() == null || g.respuesta().isBlank()) {
        continue;
      }

      Flashcard fc = new Flashcard();
      fc.setUsuario(usuario);
      fc.setTema(documento.getTema());
      fc.setDocumento(documento);
      fc.setPregunta(g.pregunta().trim());
      fc.setRespuesta(g.respuesta().trim());
      fc.setDificultad(mapDificultad(g.dificultad()));
      // En el enfoque simplificado no vinculamos flashcards a chunks (evita errores con CLOB/upper()).
      fc.setChunkOrigen(null);

      toSave.add(fc);
    }

    if (toSave.isEmpty()) {
      throw new ServiceUnavailableException("No se pudieron construir flashcards válidas");
    }

    List<Flashcard> saved = flashcardRepository.saveAll(toSave);
    log.info("Flashcards generadas: documentoId={}, usuarioId={}, count={}", documentoId, usuarioId, saved.size());
    return saved;
  }

  private boolean isDocumentoListo(Documento documento) {
    EstadoProcesadoDocumento estado = documento.getEstadoProcesado();
    return estado == EstadoProcesadoDocumento.PROCESADO || estado == EstadoProcesadoDocumento.LISTO;
  }

  private List<FlashcardGenerated> parseFlashcards(String jsonArray) {
    try {
      List<FlashcardGenerated> parsed =
          objectMapper.readValue(jsonArray, new TypeReference<List<FlashcardGenerated>>() {});
      return parsed == null ? List.of() : parsed;
    } catch (Exception ex) {
      return List.of();
    }
  }

  private int mapDificultad(String dificultad) {
    if (dificultad == null) {
      return 3;
    }
    return switch (dificultad.trim().toUpperCase(Locale.ROOT)) {
      case "BASICA" -> 2;
      case "AVANZADA" -> 4;
      default -> 3;
    };
  }

  private String truncate(String text, int maxChars) {
    return TextUtils.truncate(text, maxChars);
  }

  private record FlashcardGenerated(String pregunta, String respuesta, String dificultad) {}
}
