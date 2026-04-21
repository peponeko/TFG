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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

  private static final int KEYWORD_MAX = 6;
  private static final int CHUNKS_PER_KEYWORD = 50;
  private static final int HISTORY_MAX_MESSAGES = 10;

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
          "era",
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
          "haber",
          "hace",
          "hacia",
          "han",
          "hasta",
          "hay",
          "la",
          "las",
          "le",
          "les",
          "lo",
          "los",
          "mas",
          "más",
          "me",
          "mi",
          "mis",
          "mismo",
          "mucho",
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
          "ser",
          "si",
          "sin",
          "sobre",
          "su",
          "sus",
          "tambien",
          "también",
          "te",
          "tener",
          "tiene",
          "toda",
          "todo",
          "todos",
          "tu",
          "tus",
          "un",
          "una",
          "uno",
          "unos",
          "y",
          "ya");

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

    int maxChunks = Math.max(1, aiProperties.getMaxContextChunks());
    List<Documento> documentosActivos = resolveActiveDocuments(conversacion);

    documentosActivos =
        documentosActivos.stream()
            .filter(this::isDocumentoListo)
            .filter(d -> d.getRutaArchivo() != null && !d.getRutaArchivo().isBlank())
            .toList();

    List<Long> documentoIds = documentosActivos.stream().map(Documento::getId).toList();
    List<DocumentoChunk> chunks = retrieveRelevantChunks(contenido, documentoIds, maxChunks);

    String assistantContent;
    if (chunks.isEmpty()) {
      assistantContent = "No encuentro información sobre esto en los documentos proporcionados";
    } else {
      if (!aiService.isDisponible()) {
        throw new ServiceUnavailableException(
            "IA no disponible. Para usar el chat instala Ollama y arráncalo en "
                + aiProperties.getOllama().getBaseUrl());
      }

      String chunksText = buildChunksContext(chunks);
      String historyText = buildRecentHistory(conversacionId);
      String contexto = chunksText;
      if (!historyText.isBlank()) {
        contexto += "\n\nHISTORIAL RECIENTE (no es fuente, solo contexto conversacional):\n" + historyText;
      }

      String prompt = PromptTemplates.formatChat(contexto, contenido);
      assistantContent = aiService.generarRespuesta(prompt, aiProperties.getMaxTokensChat());
      if (assistantContent == null || assistantContent.isBlank()) {
        assistantContent = "No encuentro información sobre esto en los documentos proporcionados";
      }
    }

    List<Long> chunkIds = chunks.stream().map(DocumentoChunk::getId).filter(Objects::nonNull).toList();
    String sourcesJson = toJsonSilently(chunkIds);

    ChatMensaje assistantMessage = new ChatMensaje();
    assistantMessage.setConversacion(conversacion);
    assistantMessage.setRol(ChatRol.ASSISTANT);
    assistantMessage.setContenido(assistantContent);
    assistantMessage.setFuentesUsadasJson(sourcesJson);
    assistantMessage = mensajeRepository.save(assistantMessage);

    conversacion.setUpdatedAt(LocalDateTime.now());
    conversacionRepository.save(conversacion);

    List<ChatSource> sources =
        chunks.stream()
            .map(this::toSource)
            .filter(Objects::nonNull)
            .toList();

    log.info(
        "Mensaje procesado: conversacionId={}, usuarioId={}, chunks={}",
        conversacionId,
        usuarioId,
        chunks.size());

    return new ChatSendMessageResult(userMessage, assistantMessage, sources);
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
    if (tema == null
        || tema.getUnidad() == null
        || tema.getUnidad().getResultadoAprendizaje() == null
        || tema.getUnidad().getResultadoAprendizaje().getAsignatura() == null) {
      throw new BadRequestException("Tema inválido");
    }
    return tema.getUnidad().getResultadoAprendizaje().getAsignatura();
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

  private boolean isDocumentoListo(Documento documento) {
    if (documento == null) {
      return false;
    }
    EstadoProcesadoDocumento estado = documento.getEstadoProcesado();
    return estado == EstadoProcesadoDocumento.PROCESADO || estado == EstadoProcesadoDocumento.LISTO;
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

  private List<DocumentoChunk> retrieveRelevantChunks(String question, List<Long> documentoIds, int maxChunks) {
    if (documentoIds == null || documentoIds.isEmpty()) {
      return List.of();
    }

    List<String> keywords = extractKeywords(question);
    if (keywords.isEmpty()) {
      Page<DocumentoChunk> page =
          documentoChunkRepository.findByDocumentoIdInOrderByDocumentoIdAscIndiceChunkAsc(
              documentoIds, PageRequest.of(0, maxChunks));
      return page.getContent();
    }

    Map<Long, ScoredChunk> candidates = new HashMap<>();

    for (String kw : keywords) {
      Page<DocumentoChunk> page =
          documentoChunkRepository.findByDocumentoIdInAndTextoContainingIgnoreCase(
              documentoIds, kw, PageRequest.of(0, CHUNKS_PER_KEYWORD));

      for (DocumentoChunk chunk : page.getContent()) {
        if (chunk.getId() == null) {
          continue;
        }
        String text = chunk.getTexto() == null ? "" : chunk.getTexto();
        int occurrences = countOccurrences(normalizeForScoring(text), kw);

        ScoredChunk scored = candidates.computeIfAbsent(chunk.getId(), id -> new ScoredChunk(chunk));
        scored.matchedKeywords.add(kw);
        scored.occurrences += Math.max(1, occurrences);
      }
    }

    if (candidates.isEmpty()) {
      Page<DocumentoChunk> page =
          documentoChunkRepository.findByDocumentoIdInOrderByDocumentoIdAscIndiceChunkAsc(
              documentoIds, PageRequest.of(0, maxChunks));
      return page.getContent();
    }

    return candidates.values().stream()
        .sorted(Comparator.<ScoredChunk>comparingInt(ScoredChunk::score).reversed())
        .limit(maxChunks)
        .map(sc -> sc.chunk)
        .toList();
  }

  private String buildChunksContext(List<DocumentoChunk> chunks) {
    StringBuilder sb = new StringBuilder();
    for (DocumentoChunk c : chunks) {
      if (c == null) {
        continue;
      }

      Documento doc = c.getDocumento();
      String docName = doc != null && doc.getNombreOriginal() != null ? doc.getNombreOriginal() : "Documento";
      int fragmento = c.getIndiceChunk() != null ? c.getIndiceChunk() + 1 : 0;

      sb.append("[Doc: ").append(docName).append(", Fragmento ").append(fragmento).append("]");
      if (c.getPaginaOrigen() != null) {
        sb.append(" (Página ").append(c.getPaginaOrigen()).append(")");
      }
      sb.append('\n');

      String text = c.getTexto() == null ? "" : c.getTexto().trim();
      if (text.length() > 3500) {
        text = text.substring(0, 3400).trim() + "…";
      }
      sb.append(text).append("\n\n");
    }
    return sb.toString().trim();
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

  private List<String> extractKeywords(String question) {
    if (question == null || question.isBlank()) {
      return List.of();
    }

    String normalized = normalizeForScoring(question);
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

  private ChatSource toSource(DocumentoChunk chunk) {
    if (chunk == null || chunk.getId() == null) {
      return null;
    }
    Documento doc = chunk.getDocumento();
    Long docId = doc != null ? doc.getId() : null;
    String docName = doc != null ? doc.getNombreOriginal() : null;
    return new ChatSource(chunk.getId(), docId, docName, chunk.getIndiceChunk(), chunk.getPaginaOrigen());
  }

  private static class ScoredChunk {
    private final DocumentoChunk chunk;
    private final Set<String> matchedKeywords = new HashSet<>();
    private int occurrences = 0;

    private ScoredChunk(DocumentoChunk chunk) {
      this.chunk = chunk;
    }

    private int score() {
      return matchedKeywords.size() * 10 + occurrences;
    }
  }
}

