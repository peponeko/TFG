package com.easy4you.service.impl;

import com.easy4you.config.AiProperties;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.exception.ServiceUnavailableException;
import com.easy4you.model.entity.ArtefactoGenerado;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.Resumen;
import com.easy4you.model.entity.Tema;
import com.easy4you.model.entity.Usuario;
import com.easy4you.model.enums.EstadoArtefactoGenerado;
import com.easy4you.model.enums.EstadoProcesadoDocumento;
import com.easy4you.model.enums.OrigenResumen;
import com.easy4you.model.enums.TipoArtefactoGenerado;
import com.easy4you.repository.ArtefactoGeneradoRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.ResumenRepository;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.service.ResumenGenerationService;
import com.easy4you.service.TemaService;
import com.easy4you.service.ai.AiService;
import com.easy4you.util.PromptTemplates;
import com.easy4you.util.TextUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumenGenerationServiceImpl implements ResumenGenerationService {

  private static final Logger log = LoggerFactory.getLogger(ResumenGenerationServiceImpl.class);

  private static final String MSG_DOC_NO_PROCESADO =
      "El documento aún no está procesado. Espera unos segundos y recarga la página.";

  private static final int MAX_INPUT_CHARS_DOCUMENTO = 12_000;
  private static final int MAX_INPUT_CHARS_TEMA = 18_000;
  private static final int MAX_PUNTOS_CLAVE = 5;

  private final ArtefactoGeneradoRepository artefactoGeneradoRepository;
  private final DocumentoRepository documentoRepository;
  private final ResumenRepository resumenRepository;
  private final UsuarioRepository usuarioRepository;
  private final TemaService temaService;
  private final AiService aiService;
  private final AiProperties aiProperties;
  private final ObjectMapper objectMapper;
  private final TaskExecutor taskExecutor;

  @Override
  public Resumen generarResumenDocumento(Long usuarioId, Long documentoId) {
    return generarResumenDocumentoInternal(usuarioId, documentoId, true);
  }

  @Override
  public void solicitarResumenDocumento(Long usuarioId, Long documentoId) {
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
      throw new BadRequestException("El documento no tiene tema. Asigna un tema antes de generar resúmenes.");
    }

    documento.setEstadoProcesado(EstadoProcesadoDocumento.PROCESANDO);
    documento.setErrorExtraccion(null);
    documentoRepository.save(documento);

    taskExecutor.execute(() -> generarResumenDocumentoAsync(usuarioId, documentoId));
  }

  @Async
  @Transactional
  @Override
  public void generarResumenDocumentoAsync(Long usuarioId, Long documentoId) {
    try {
      generarResumenDocumentoInternal(usuarioId, documentoId, false);
      updateDocumentoEstado(usuarioId, documentoId, EstadoProcesadoDocumento.LISTO, null);
    } catch (Exception ex) {
      log.error("Error generando resumen: documentoId={}, usuarioId={}", documentoId, usuarioId, ex);
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

  private Resumen generarResumenDocumentoInternal(Long usuarioId, Long documentoId, boolean validarEstado) {
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
      throw new BadRequestException("El documento no tiene tema. Asigna un tema antes de generar resúmenes.");
    }

    String input = truncate(documento.getTextoExtraido(), MAX_INPUT_CHARS_DOCUMENTO);
    String prompt = PromptTemplates.formatResumen(input);

    String content = aiService.generarRespuesta(prompt, aiProperties.getMaxTokensResumen());
    if (content == null || content.isBlank()) {
      throw new ServiceUnavailableException("La IA no devolvió una respuesta válida. Inténtalo de nuevo.");
    }

    List<String> puntosClave = extractPuntosClave(content);

    Resumen resumen = new Resumen();
    resumen.setUsuario(usuario);
    resumen.setTema(documento.getTema());
    resumen.setDocumento(documento);
    resumen.setTitulo(buildTituloDocumento(documento));
    resumen.setContenido(content.trim());
    resumen.setPuntosClaveJson(toJsonSilently(puntosClave));
    resumen.setOrigen(OrigenResumen.GENERADO);

    Resumen saved = resumenRepository.save(resumen);
    log.info("Resumen generado: resumenId={}, documentoId={}, usuarioId={}", saved.getId(), documentoId, usuarioId);
    return saved;
  }

  @Override
  public Resumen generarResumenTema(Long usuarioId, Long temaId) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (temaId == null) {
      throw new BadRequestException("temaId es obligatorio");
    }

    Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + usuarioId));

    Tema tema = temaService.obtenerPorId(temaId);

    Long temaUsuarioId =
        tema.getAsignatura() != null && tema.getAsignatura().getUsuario() != null
            ? tema.getAsignatura().getUsuario().getId()
            : null;

    if (temaUsuarioId == null || !usuarioId.equals(temaUsuarioId)) {
      throw new NotFoundException("Tema no encontrado: " + temaId);
    }

    List<Documento> documentos = documentoRepository.findByTemaId(temaId).stream()
        .filter(d -> d.getUsuario() != null && usuarioId.equals(d.getUsuario().getId()))
        .filter(this::isDocumentoListo)
        .filter(d -> d.getTextoExtraido() != null && !d.getTextoExtraido().isBlank())
        .toList();

    if (documentos.isEmpty()) {
      throw new BadRequestException("No hay documentos procesados con texto para este tema");
    }

    String combined = buildCombinedTextForTema(documentos);
    String input = truncate(combined, MAX_INPUT_CHARS_TEMA);
    String prompt = PromptTemplates.formatResumen(input);

    String content = aiService.generarRespuesta(prompt, aiProperties.getMaxTokensResumen());
    if (content == null || content.isBlank()) {
      throw new ServiceUnavailableException("La IA no devolvió una respuesta válida. Inténtalo de nuevo.");
    }

    List<String> puntosClave = extractPuntosClave(content);

    Resumen resumen = new Resumen();
    resumen.setUsuario(usuario);
    resumen.setTema(tema);
    resumen.setDocumento(null);
    resumen.setTitulo(buildTituloTema(tema));
    resumen.setContenido(content.trim());
    resumen.setPuntosClaveJson(toJsonSilently(puntosClave));
    resumen.setOrigen(OrigenResumen.GENERADO);

    Resumen saved = resumenRepository.save(resumen);
    log.info("Resumen de tema generado: resumenId={}, temaId={}, usuarioId={}", saved.getId(), temaId, usuarioId);
    return saved;
  }

  @Override
  public ArtefactoGenerado generarResumenConArtefacto(Long usuarioId, Long documentoId) {
    Resumen resumen = generarResumenDocumento(usuarioId, documentoId);

    if (resumen.getTema() == null || resumen.getTema().getAsignatura() == null) {
      throw new BadRequestException("El documento no tiene asignatura");
    }

    ArtefactoGenerado artefacto = new ArtefactoGenerado();
    artefacto.setAsignatura(resumen.getTema().getAsignatura());
    artefacto.setTipo(TipoArtefactoGenerado.RESUMEN);
    artefacto.setEstado(EstadoArtefactoGenerado.LISTO);
    artefacto.setMetadatosJson(toJsonSilently(Map.of(
        "resumenId", resumen.getId(),
        "documentoId", documentoId,
        "titulo", resumen.getTitulo()
    )));

    ArtefactoGenerado savedArtefacto = artefactoGeneradoRepository.save(artefacto);
    log.info("Artefacto de resumen generado: artefactoId={}, resumenId={}", savedArtefacto.getId(), resumen.getId());
    return savedArtefacto;
  }

  private boolean isDocumentoListo(Documento documento) {
    EstadoProcesadoDocumento estado = documento.getEstadoProcesado();
    return estado == EstadoProcesadoDocumento.PROCESADO || estado == EstadoProcesadoDocumento.LISTO;
  }

  private String buildTituloDocumento(Documento documento) {
    String base = documento.getNombreOriginal() == null ? "Documento" : documento.getNombreOriginal().trim();
    String title = "Resumen · " + base;
    if (title.length() <= 200) {
      return title;
    }
    return title.substring(0, 197) + "…";
  }

  private String buildTituloTema(Tema tema) {
    String base = tema.getTitulo() == null ? "Tema" : tema.getTitulo().trim();
    String title = "Resumen del tema · " + base;
    if (title.length() <= 200) {
      return title;
    }
    return title.substring(0, 197) + "…";
  }

  private String buildCombinedTextForTema(List<Documento> documentos) {
    StringBuilder sb = new StringBuilder();
    for (Documento d : documentos) {
      String name = d.getNombreOriginal() == null ? "Documento" : d.getNombreOriginal().trim();
      sb.append("DOCUMENTO: ").append(name).append("\n");
      String text = truncate(d.getTextoExtraido(), 9_000);
      sb.append(text).append("\n\n");
    }
    return sb.toString().trim();
  }

  private List<String> extractPuntosClave(String content) {
    if (content == null || content.isBlank()) {
      return List.of();
    }

    List<String> puntos = new ArrayList<>();
    String[] lines = content.split("\\r?\\n");
    for (String line : lines) {
      String t = line == null ? "" : line.trim();
      if (t.isBlank()) {
        continue;
      }

      if (t.startsWith("•")) {
        t = t.substring(1).trim();
      } else if (t.startsWith("-")) {
        t = t.substring(1).trim();
      } else if (t.startsWith("*")) {
        t = t.substring(1).trim();
      } else {
        continue;
      }

      if (t.isBlank()) {
        continue;
      }

      if (t.length() > 220) {
        t = t.substring(0, 210).trim() + "…";
      }

      puntos.add(capitalize(t));
      if (puntos.size() >= MAX_PUNTOS_CLAVE) {
        break;
      }
    }

    return puntos;
  }

  private String capitalize(String s) {
    if (s == null || s.isBlank()) {
      return "";
    }
    String t = s.trim();
    if (t.length() == 1) {
      return t.toUpperCase(Locale.ROOT);
    }
    return t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1);
  }

  private String truncate(String text, int maxChars) {
    return TextUtils.truncate(text, maxChars);
  }

  private String toJsonSilently(Object obj) {
    if (obj == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception ex) {
      return null;
    }
  }
}
