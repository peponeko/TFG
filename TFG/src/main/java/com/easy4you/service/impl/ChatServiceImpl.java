package com.easy4you.service.impl;

import com.easy4you.config.AiProperties;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.exception.ServiceUnavailableException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.ChatConversacion;
import com.easy4you.model.entity.ChatMensaje;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.DocumentoChunk;
import com.easy4you.model.entity.NotebookCompartido;
import com.easy4you.model.entity.Tema;
import com.easy4you.model.entity.Usuario;
import com.easy4you.model.enums.ChatRol;
import com.easy4you.model.enums.EstadoProcesadoDocumento;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.ChatConversacionRepository;
import com.easy4you.repository.ChatMensajeRepository;
import com.easy4you.repository.DocumentoChunkRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.NotebookCompartidoRepository;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.service.ChatSendMessageResult;
import com.easy4you.service.ChatService;
import com.easy4you.service.ChatSource;
import com.easy4you.service.TemaService;
import com.easy4you.service.ai.AiService;
import com.easy4you.util.PromptTemplates;
import com.easy4you.util.TextUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

  private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

  private static final int MAX_INPUT_CHARS_CHAT = 10_000;
  private static final int CONTEXT_MAX_CHUNKS = 14;
  private static final int CONTEXT_MAX_CHUNK_CHARS = 800;
  private static final int OVERVIEW_CHUNKS_PER_DOC = 4;

  private static final int HISTORY_MAX_MESSAGES = 10;

  private final ChatConversacionRepository conversacionRepository;
  private final ChatMensajeRepository mensajeRepository;
  private final UsuarioRepository usuarioRepository;
  private final AsignaturaRepository asignaturaRepository;
  private final NotebookCompartidoRepository notebookCompartidoRepository;
  private final TemaService temaService;
  private final DocumentoRepository documentoRepository;
  private final DocumentoChunkRepository documentoChunkRepository;
  private final AiService aiService;
  private final AiProperties aiProperties;
  private final ObjectMapper objectMapper;

  @Override
  public ChatConversacion crearConversacion(Long usuarioId, Long asignaturaId, Long temaId, String titulo) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (asignaturaId == null && temaId == null) {
      throw new BadRequestException("Debe indicar asignaturaId o temaId");
    }

    Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + usuarioId));

    Asignatura asignatura = null;
    Tema tema = null;

    if (temaId != null) {
      tema = temaService.obtenerPorId(temaId);
      asignatura = resolveAsignaturaFromTema(tema);
      ensureAsignaturaAccessibleByUsuario(asignatura, usuarioId);
    } else {
      asignatura = resolveAsignaturaAccessible(asignaturaId, usuarioId);
    }

    String resolvedTitle = (titulo == null || titulo.isBlank()) ? defaultTitle(asignatura, tema) : titulo.trim();

    ChatConversacion conversacion = new ChatConversacion();
    conversacion.setUsuario(usuario);
    conversacion.setAsignatura(asignatura);
    conversacion.setTema(tema);
    conversacion.setTitulo(resolvedTitle);
    conversacion.setFuentesActivasJson(null);

    ChatConversacion saved = conversacionRepository.save(conversacion);

    log.info(
        "Conversación creada: id={}, usuarioId={}, asignaturaId={}, temaId={}",
        saved.getId(),
        usuarioId,
        asignatura != null ? asignatura.getId() : null,
        tema != null ? tema.getId() : null);

    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChatConversacion> listarConversaciones(Long usuarioId) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    return conversacionRepository.findByUsuarioIdOrderByUpdatedAtDesc(usuarioId);
  }

  @Override
  @Transactional(readOnly = true)
  public ChatConversacion obtenerConversacion(Long usuarioId, Long conversacionId) {
    ChatConversacion conversacion = resolveConversacionOrThrow(usuarioId, conversacionId);
    return conversacion;
  }

  @Override
  public void eliminarConversacion(Long usuarioId, Long conversacionId) {
    ChatConversacion conversacion = resolveConversacionOrThrow(usuarioId, conversacionId);
    conversacionRepository.delete(conversacion);
    log.info("Conversación eliminada: id={}, usuarioId={}", conversacionId, usuarioId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChatMensaje> listarMensajes(Long usuarioId, Long conversacionId) {
    resolveConversacionOrThrow(usuarioId, conversacionId);
    return mensajeRepository.findByConversacionIdOrderByCreatedAtAsc(conversacionId);
  }

  @Override
  public ChatSendMessageResult enviarMensaje(Long usuarioId, Long conversacionId, String contenido) {
    if (contenido == null || contenido.isBlank()) {
      throw new BadRequestException("El contenido es obligatorio");
    }

    ChatConversacion conversacion = resolveConversacionOrThrow(usuarioId, conversacionId);

    ChatMensaje userMessage = new ChatMensaje();
    userMessage.setConversacion(conversacion);
    userMessage.setRol(ChatRol.USER);
    userMessage.setContenido(contenido.trim());
    userMessage.setFuentesUsadasJson(null);
    userMessage = mensajeRepository.save(userMessage);

    List<Documento> documentosActivos = resolveActiveDocuments(conversacion);

    documentosActivos =
        documentosActivos.stream()
            .filter(d -> d.getRutaArchivo() != null && !d.getRutaArchivo().isBlank())
            .filter(this::isDocumentoUsableForChat)
            .toList();

    String documentosContexto =
        isGeneralOverviewQuestion(contenido)
            ? buildOverviewContextFromFirstChunks(documentosActivos)
            : buildRelevantContextFromChunks(documentosActivos, contenido);
    String assistantContent = generarContenidoAsistente(conversacionId, contenido, documentosContexto);

    ChatMensaje assistantMessage = new ChatMensaje();
    assistantMessage.setConversacion(conversacion);
    assistantMessage.setRol(ChatRol.ASSISTANT);
    assistantMessage.setContenido(assistantContent);
    assistantMessage.setFuentesUsadasJson(null);
    assistantMessage = mensajeRepository.save(assistantMessage);

    conversacion.setUpdatedAt(LocalDateTime.now());
    conversacionRepository.save(conversacion);

    List<ChatSource> sources = List.of();

    log.info(
        "Mensaje procesado: conversacionId={}, usuarioId={}, documentosActivos={}",
        conversacionId,
        usuarioId,
        documentosActivos.size());

    return new ChatSendMessageResult(userMessage, assistantMessage, sources);
  }

  private String generarContenidoAsistente(Long conversacionId, String contenido, String documentosContexto) {
    // Modo "chat normal": si no hay contexto suficiente, responde igualmente (estilo ChatGPT),
    // avisando que no se han usado documentos.
    if (documentosContexto == null || documentosContexto.isBlank()) {
      String prompt =
          """
          El usuario está usando un asistente tipo ChatGPT.
          No hay texto de documentos disponible para esta respuesta.
          Responde en español de forma útil y clara.
          Si el usuario pregunta sobre un PDF/documento, sugiere subir/seleccionar un documento o hacer una pregunta más concreta.
          
          Mensaje del usuario:
          """
              + (contenido == null ? "" : contenido.trim());
      String out = aiService.generarRespuesta(prompt, aiProperties.getMaxTokensChat());
      return (out == null || out.isBlank()) ? "Hola. ¿En qué puedo ayudarte?" : out;
    }

    String historyText = buildRecentHistory(conversacionId);
    String contexto = documentosContexto;
    if (!historyText.isBlank()) {
      contexto += "\n\nHISTORIAL RECIENTE (no es fuente, solo contexto conversacional):\n" + historyText;
    }
    contexto = truncate(contexto, MAX_INPUT_CHARS_CHAT);

    String prompt = PromptTemplates.formatChat(contexto, contenido);
    String assistantContent = aiService.generarRespuesta(prompt, aiProperties.getMaxTokensChat());
    if (assistantContent == null || assistantContent.isBlank()) {
      throw new ServiceUnavailableException("La IA no devolvió una respuesta válida. Inténtalo de nuevo.");
    }
    return assistantContent;
  }

  @Override
  public ChatConversacion actualizarFuentes(Long usuarioId, Long conversacionId, List<Long> documentoIds) {
    ChatConversacion conversacion = resolveConversacionOrThrow(usuarioId, conversacionId);

    if (documentoIds == null || documentoIds.isEmpty()) {
      conversacion.setFuentesActivasJson(null);
      return conversacionRepository.save(conversacion);
    }

    List<Long> uniqueIds = documentoIds.stream().filter(Objects::nonNull).distinct().toList();
    List<Documento> fetched = documentoRepository.findAllById(uniqueIds);
    if (fetched.size() != uniqueIds.size()) {
      throw new BadRequestException("Algún documento no existe");
    }

    for (Documento d : fetched) {
      ensureDocumentoInScope(conversacion, d);
    }

    conversacion.setFuentesActivasJson(toJsonSilently(uniqueIds));
    return conversacionRepository.save(conversacion);
  }

  private ChatConversacion resolveConversacionOrThrow(Long usuarioId, Long conversacionId) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (conversacionId == null) {
      throw new BadRequestException("conversacionId es obligatorio");
    }

    ChatConversacion conversacion =
        conversacionRepository
            .findById(conversacionId)
            .orElseThrow(() -> new NotFoundException("Conversación no encontrada: " + conversacionId));

    if (conversacion.getUsuario() == null || !usuarioId.equals(conversacion.getUsuario().getId())) {
      throw new NotFoundException("Conversación no encontrada: " + conversacionId);
    }

    // Validar que la asignatura/tema sigue siendo accesible
    if (conversacion.getTema() != null) {
      Asignatura a = resolveAsignaturaFromTema(conversacion.getTema());
      ensureAsignaturaAccessibleByUsuario(a, usuarioId);
    } else if (conversacion.getAsignatura() != null) {
      ensureAsignaturaAccessibleByUsuario(conversacion.getAsignatura(), usuarioId);
    }

    return conversacion;
  }

  private Asignatura resolveAsignaturaAccessible(Long asignaturaId, Long usuarioId) {
    if (asignaturaId == null) {
      throw new BadRequestException("asignaturaId es obligatorio");
    }

    Optional<Asignatura> owned = asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioId);
    if (owned.isPresent()) {
      return owned.get();
    }

    Optional<NotebookCompartido> shared =
        notebookCompartidoRepository.findByAsignaturaIdAndUsuarioInvitadoId(asignaturaId, usuarioId);
    if (shared.isPresent()) {
      return shared.get().getAsignatura();
    }

    throw new NotFoundException("Asignatura no encontrada: " + asignaturaId);
  }

  private void ensureAsignaturaAccessibleByUsuario(Asignatura asignatura, Long usuarioId) {
    if (asignatura == null || asignatura.getId() == null) {
      throw new BadRequestException("Asignatura inválida");
    }

    if (asignatura.getUsuario() != null && usuarioId.equals(asignatura.getUsuario().getId())) {
      return;
    }

    if (notebookCompartidoRepository
        .findByAsignaturaIdAndUsuarioInvitadoId(asignatura.getId(), usuarioId)
        .isPresent()) {
      return;
    }

    throw new NotFoundException("Asignatura no encontrada: " + asignatura.getId());
  }

  private Asignatura resolveAsignaturaFromTema(Tema tema) {
    if (tema == null || tema.getAsignatura() == null || tema.getAsignatura().getId() == null) {
      throw new BadRequestException("Tema inválido");
    }
    return tema.getAsignatura();
  }

  private String defaultTitle(Asignatura asignatura, Tema tema) {
    if (tema != null && tema.getTitulo() != null && !tema.getTitulo().isBlank()) {
      return "Chat · " + tema.getTitulo().trim();
    }
    if (asignatura != null && asignatura.getNombre() != null && !asignatura.getNombre().isBlank()) {
      return "Chat · " + asignatura.getNombre().trim();
    }
    return "Chat";
  }

  private boolean isDocumentoUsableForChat(Documento documento) {
    if (documento == null) {
      return false;
    }
    String texto = documento.getTextoExtraido();
    if (texto == null || texto.isBlank()) {
      return false;
    }
    EstadoProcesadoDocumento estado = documento.getEstadoProcesado();
    return estado == EstadoProcesadoDocumento.PROCESADO
        || estado == EstadoProcesadoDocumento.LISTO
        || estado == EstadoProcesadoDocumento.PROCESANDO;
  }

  private List<Documento> resolveActiveDocuments(ChatConversacion conversacion) {
    List<Long> ids = parseLongList(conversacion.getFuentesActivasJson());
    if (ids != null && !ids.isEmpty()) {
      List<Documento> fetched = documentoRepository.findAllById(ids);
      fetched.forEach(d -> ensureDocumentoInScope(conversacion, d));
      return fetched;
    }

    if (conversacion.getTema() != null && conversacion.getTema().getId() != null) {
      return documentoRepository.findByTemaId(conversacion.getTema().getId());
    }

    if (conversacion.getAsignatura() != null && conversacion.getAsignatura().getId() != null) {
      return documentoRepository.findByAsignaturaId(conversacion.getAsignatura().getId());
    }

    return List.of();
  }

  private void ensureDocumentoInScope(ChatConversacion conversacion, Documento documento) {
    if (documento == null || documento.getId() == null) {
      throw new BadRequestException("Documento inválido");
    }

    if (conversacion.getAsignatura() != null && conversacion.getAsignatura().getId() != null) {
      if (documento.getAsignatura() == null
          || documento.getAsignatura().getId() == null
          || !conversacion.getAsignatura().getId().equals(documento.getAsignatura().getId())) {
        throw new BadRequestException("Documento fuera del notebook de la conversación");
      }
    }

    if (conversacion.getTema() != null && conversacion.getTema().getId() != null) {
      Long temaId = conversacion.getTema().getId();
      if (documento.getTema() == null || documento.getTema().getId() == null || !temaId.equals(documento.getTema().getId())) {
        throw new BadRequestException("Documento fuera del tema de la conversación");
      }
    }
  }

  private String buildRelevantContextFromChunks(List<Documento> documentosActivos, String pregunta) {
    if (documentosActivos == null || documentosActivos.isEmpty()) {
      return "";
    }
    String q = pregunta == null ? "" : pregunta.trim();
    if (q.isBlank()) {
      return "";
    }

    List<Long> docIds =
        documentosActivos.stream()
            .filter(Objects::nonNull)
            .map(Documento::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    if (docIds.isEmpty()) {
      return "";
    }

    Page<DocumentoChunk> page;
    try {
      page = documentoChunkRepository.searchFullText(docIds, q, PageRequest.of(0, CONTEXT_MAX_CHUNKS));
    } catch (Exception ex) {
      page =
          documentoChunkRepository.findByDocumentoIdInAndTextoContaining(
              docIds, q, PageRequest.of(0, CONTEXT_MAX_CHUNKS));
    }

    if (page.getContent() == null || page.getContent().isEmpty()) {
      // Fallback: si no hay match, al menos mandamos un pequeño prefijo del texto extraído
      // para no devolver siempre el mensaje de "no encuentro".
      StringBuilder sb = new StringBuilder();
      for (Documento d : documentosActivos) {
        if (d == null) continue;
        String nombre =
            d.getNombreOriginal() == null || d.getNombreOriginal().isBlank() ? "Documento" : d.getNombreOriginal().trim();
        String texto = d.getTextoExtraido() == null ? "" : d.getTextoExtraido().trim();
        if (texto.isBlank()) continue;
        sb.append("[Doc: ").append(nombre).append("]\n");
        sb.append(truncate(texto, 1800)).append("\n\n");
      }
      return sb.toString().trim();
    }

    StringBuilder sb = new StringBuilder();
    for (DocumentoChunk c : page.getContent()) {
      if (c == null || c.getDocumento() == null) {
        continue;
      }
      String nombre =
          c.getDocumento().getNombreOriginal() == null || c.getDocumento().getNombreOriginal().isBlank()
              ? "Documento"
              : c.getDocumento().getNombreOriginal().trim();
      String texto = c.getTexto() == null ? "" : c.getTexto().trim();
      if (texto.isBlank()) {
        continue;
      }
      sb.append("[Doc: ").append(nombre).append("]\n");
      sb.append(truncate(texto, CONTEXT_MAX_CHUNK_CHARS)).append("\n\n");
    }
    return sb.toString().trim();
  }

  private String buildOverviewContextFromFirstChunks(List<Documento> documentosActivos) {
    if (documentosActivos == null || documentosActivos.isEmpty()) {
      return "";
    }
    List<Long> docIds =
        documentosActivos.stream()
            .filter(Objects::nonNull)
            .map(Documento::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (docIds.isEmpty()) {
      return "";
    }

    Page<DocumentoChunk> page =
        documentoChunkRepository.findByDocumentoIdInOrderByDocumentoIdAscIndiceChunkAsc(
            docIds, PageRequest.of(0, Math.min(CONTEXT_MAX_CHUNKS, Math.max(6, docIds.size() * OVERVIEW_CHUNKS_PER_DOC))));

    if (page.getContent() == null || page.getContent().isEmpty()) {
      // Fallback final: prefijo del texto extraído
      StringBuilder sb = new StringBuilder();
      for (Documento d : documentosActivos) {
        if (d == null) continue;
        String nombre =
            d.getNombreOriginal() == null || d.getNombreOriginal().isBlank() ? "Documento" : d.getNombreOriginal().trim();
        String texto = d.getTextoExtraido() == null ? "" : d.getTextoExtraido().trim();
        if (texto.isBlank()) continue;
        sb.append("[Doc: ").append(nombre).append("]\n");
        sb.append(truncate(texto, 2000)).append("\n\n");
      }
      return sb.toString().trim();
    }

    StringBuilder sb = new StringBuilder();
    for (DocumentoChunk c : page.getContent()) {
      if (c == null || c.getDocumento() == null) continue;
      String nombre =
          c.getDocumento().getNombreOriginal() == null || c.getDocumento().getNombreOriginal().isBlank()
              ? "Documento"
              : c.getDocumento().getNombreOriginal().trim();
      String texto = c.getTexto() == null ? "" : c.getTexto().trim();
      if (texto.isBlank()) continue;
      sb.append("[Doc: ").append(nombre).append("]\n");
      sb.append(truncate(texto, CONTEXT_MAX_CHUNK_CHARS)).append("\n\n");
    }
    return sb.toString().trim();
  }

  private boolean isGeneralOverviewQuestion(String contenido) {
    if (contenido == null) return false;
    String t = contenido.trim().toLowerCase();
    if (t.isBlank()) return false;
    // Saludos / mensajes muy cortos: tratarlos como "general"
    if (t.length() < 8) return true;
    if (t.equals("hola") || t.equals("buenas") || t.equals("hey") || t.equals("holaa")) return true;

    return t.contains("de qué va")
        || t.contains("de que va")
        || t.contains("resumen")
        || t.contains("cosas más importantes")
        || t.contains("cosas mas importantes")
        || t.contains("lo más importante")
        || t.contains("lo mas importante")
        || t.contains("puntos clave")
        || t.contains("ideas principales")
        || t.contains("explica el pdf")
        || t.contains("explicame el pdf")
        || t.contains("explica el documento")
        || t.contains("explicame el documento");
  }

  private String buildRecentHistory(Long conversacionId) {
    if (conversacionId == null) {
      return "";
    }

    List<ChatMensaje> all = mensajeRepository.findByConversacionIdOrderByCreatedAtAsc(conversacionId);
    if (all.isEmpty()) {
      return "";
    }

    int from = Math.max(0, all.size() - HISTORY_MAX_MESSAGES);
    List<ChatMensaje> recent = all.subList(from, all.size());

    StringBuilder sb = new StringBuilder();
    for (ChatMensaje m : recent) {
      if (m == null || m.getRol() == null) {
        continue;
      }
      if (m.getRol() == ChatRol.SYSTEM) {
        continue;
      }
      String role = m.getRol() == ChatRol.USER ? "USER" : "ASSISTANT";
      sb.append(role).append(": ");

      String content = m.getContenido() == null ? "" : m.getContenido().trim();
      if (content.length() > 600) {
        content = content.substring(0, 580).trim() + "…";
      }
      sb.append(content).append('\n');
    }
    return sb.toString().trim();
  }

  private List<Long> parseLongList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<Long> list = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
      if (list == null) {
        return List.of();
      }
      return list.stream().filter(Objects::nonNull).distinct().toList();
    } catch (Exception ex) {
      return List.of();
    }
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

  private String truncate(String text, int maxChars) {
    return TextUtils.truncate(text, maxChars);
  }
}
