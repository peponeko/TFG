package com.easy4you.service.impl;

import com.easy4you.dto.documento.DocumentoBusquedaFragmentDTO;
import com.easy4you.dto.documento.DocumentoBusquedaResultadoDTO;
import com.easy4you.dto.documento.DocumentoChunkResponseDTO;
import com.easy4you.dto.documento.DocumentoChunksPageResponseDTO;
import com.easy4you.dto.documento.DocumentoDetalleResponseDTO;
import com.easy4you.dto.documento.DocumentoEstadoResponseDTO;
import com.easy4you.dto.documento.DocumentoResponseDTO;
import com.easy4you.dto.flashcard.FlashcardResponseDTO;
import com.easy4you.dto.pregunta.PreguntaTestResponseDTO;
import com.easy4you.dto.resumen.ResumenResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.mapper.DocumentoMapper;
import com.easy4you.mapper.FlashcardMapper;
import com.easy4you.mapper.PreguntaTestMapper;
import com.easy4you.mapper.ResumenMapper;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.DocumentoChunk;
import com.easy4you.model.entity.Tema;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.DocumentoChunkRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.repository.PreguntaTestOpcionRepository;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.repository.ResumenRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.service.DocumentoService;
import com.easy4you.util.TextUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentoServiceImpl implements DocumentoService {

  private static final int SEARCH_MAX_CHUNKS = 30;
  private static final int SEARCH_MAX_FRAGMENTS_PER_DOC = 3;
  private static final int SEARCH_SNIPPET_RADIUS = 180;

  private final DocumentoRepository documentoRepository;
  private final DocumentoChunkRepository documentoChunkRepository;
  private final ResumenRepository resumenRepository;
  private final FlashcardRepository flashcardRepository;
  private final PreguntaTestRepository preguntaTestRepository;
  private final PreguntaTestOpcionRepository preguntaTestOpcionRepository;
  private final AsignaturaRepository asignaturaRepository;
  private final TemaRepository temaRepository;

  @Override
  @Transactional(readOnly = true)
  public List<Documento> listar() {
    return documentoRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Documento> listarPorUsuarioId(Long usuarioId) {
    return documentoRepository.findByUsuarioId(usuarioId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Documento> listarPorTemaId(Long temaId) {
    return documentoRepository.findByTemaId(temaId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Documento> listarPorAsignaturaIdDeUsuario(Long usuarioId, Long asignaturaId) {
    if (usuarioId == null || asignaturaId == null) {
      return List.of();
    }
    if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioId).isEmpty()) {
      throw new NotFoundException("Asignatura no encontrada: " + asignaturaId);
    }
    return documentoRepository.findByAsignaturaId(asignaturaId).stream()
        .filter(d -> d.getUsuario() != null && usuarioId.equals(d.getUsuario().getId()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DocumentoResponseDTO> listarItemsDto(Long usuarioActualId, Long temaId, Long asignaturaId) {
    if (usuarioActualId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }

    List<Documento> documentos;
    if (temaId != null) {
      documentos =
          documentoRepository.findByTemaIdFetchingRelations(temaId).stream()
              .filter(d -> d.getUsuario() != null && usuarioActualId.equals(d.getUsuario().getId()))
              .toList();
    } else if (asignaturaId != null) {
      if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioActualId).isEmpty()) {
        throw new NotFoundException("Asignatura no encontrada: " + asignaturaId);
      }
      documentos =
          documentoRepository.findByAsignaturaIdFetchingRelations(asignaturaId).stream()
              .filter(d -> d.getUsuario() != null && usuarioActualId.equals(d.getUsuario().getId()))
              .toList();
    } else {
      documentos = documentoRepository.findByUsuarioIdFetchingRelations(usuarioActualId);
    }

    return documentos.stream().map(DocumentoMapper::toListItem).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public DocumentoResponseDTO obtenerResponsePorIdDeUsuario(Long usuarioId, Long documentoId) {
    Documento d = obtenerPorIdDeUsuario(usuarioId, documentoId);
    return DocumentoMapper.toResponse(d);
  }

  @Override
  public Documento crear(Documento documento) {
    return documentoRepository.save(documento);
  }

  @Override
  @Transactional(readOnly = true)
  public Documento obtenerPorId(Long id) {
    return documentoRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));
  }

  @Override
  public Documento actualizar(Long id, Documento datos) {
    Documento existente = obtenerPorId(id);
    existente.setNombreOriginal(datos.getNombreOriginal());
    existente.setRutaArchivo(datos.getRutaArchivo());
    existente.setMimeType(datos.getMimeType());
    existente.setExtension(datos.getExtension());
    existente.setTamanoBytes(datos.getTamanoBytes());
    existente.setChecksumSha256(datos.getChecksumSha256());
    existente.setTextoExtraido(datos.getTextoExtraido());
    existente.setPaginas(datos.getPaginas());
    existente.setEstadoProcesado(datos.getEstadoProcesado());
    existente.setErrorExtraccion(datos.getErrorExtraccion());

    if (datos.getTema() != null) {
      existente.setTema(datos.getTema());
    }

    return documentoRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!documentoRepository.existsById(id)) {
      throw new NotFoundException("Documento no encontrado: " + id);
    }
    documentoRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Documento obtenerPorIdDeUsuario(Long usuarioId, Long documentoId) {
    return documentoRepository
        .findByIdAndUsuarioId(documentoId, usuarioId)
        .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + documentoId));
  }

  @Override
  @Transactional(readOnly = true)
  public DocumentoEstadoResponseDTO estado(Long usuarioId, Long documentoId) {
    Documento documento = obtenerPorIdDeUsuario(usuarioId, documentoId);

    long resumenes = resumenRepository.countByDocumentoId(documento.getId());
    long flashcards = flashcardRepository.countByDocumentoId(documento.getId());
    long preguntas = preguntaTestRepository.countByDocumentoId(documento.getId());

    return new DocumentoEstadoResponseDTO(
        documento.getId(),
        documento.getEstadoProcesado(),
        documento.getErrorExtraccion(),
        resumenes,
        flashcards,
        preguntas);
  }

  @Override
  @Transactional(readOnly = true)
  public DocumentoChunksPageResponseDTO chunks(Long usuarioId, Long documentoId, Pageable pageable) {
    Documento documento = obtenerPorIdDeUsuario(usuarioId, documentoId);

    Page<DocumentoChunk> page =
        documentoChunkRepository.findByDocumentoIdOrderByIndiceChunkAsc(documento.getId(), pageable);

    List<DocumentoChunkResponseDTO> items =
        page.getContent().stream()
            .map(
                c ->
                    new DocumentoChunkResponseDTO(
                        c.getId(),
                        documento.getId(),
                        c.getIndiceChunk(),
                        c.getTexto(),
                        c.getPaginaOrigen(),
                        c.getTokenCount()))
            .toList();

    return new DocumentoChunksPageResponseDTO(
        items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public List<DocumentoBusquedaResultadoDTO> buscar(Long usuarioId, String q, Long asignaturaId, Long temaId) {
    String query = q == null ? "" : q.trim();
    if (query.isBlank()) {
      throw new BadRequestException("q es obligatorio");
    }

    List<Documento> documentosScope = resolveDocumentScope(usuarioId, asignaturaId, temaId);
    List<Long> documentoIds = documentosScope.stream().map(Documento::getId).filter(Objects::nonNull).toList();
    if (documentoIds.isEmpty()) {
      return List.of();
    }

    Map<Long, String> docNames = new HashMap<>();
    for (Documento d : documentosScope) {
      if (d.getId() == null) {
        continue;
      }
      docNames.put(d.getId(), d.getNombreOriginal());
    }

    // Intentar búsqueda FULLTEXT primero, fallback a LIKE si falla
    Page<DocumentoChunk> page;
    try {
      page = documentoChunkRepository.searchFullText(
          documentoIds, query, PageRequest.of(0, SEARCH_MAX_CHUNKS));
    } catch (Exception e) {
      // Fallback a búsqueda LIKE si FULLTEXT no está disponible
      page = documentoChunkRepository.findByDocumentoIdInAndTextoContaining(
          documentoIds, query, PageRequest.of(0, SEARCH_MAX_CHUNKS));
    }

    LinkedHashMap<Long, List<DocumentoBusquedaFragmentDTO>> grouped = new LinkedHashMap<>();
    for (DocumentoChunk c : page.getContent()) {
      if (c == null || c.getDocumento() == null || c.getDocumento().getId() == null) {
        continue;
      }

      Long docId = c.getDocumento().getId();
      List<DocumentoBusquedaFragmentDTO> fragments = grouped.computeIfAbsent(docId, id -> new ArrayList<>());
      if (fragments.size() >= SEARCH_MAX_FRAGMENTS_PER_DOC) {
        continue;
      }

      String snippet = buildSnippetHighlighted(c.getTexto(), query);
      fragments.add(
          new DocumentoBusquedaFragmentDTO(c.getId(), c.getIndiceChunk(), c.getPaginaOrigen(), snippet));
    }

    return grouped.entrySet().stream()
        .map(
            e ->
                new DocumentoBusquedaResultadoDTO(
                    e.getKey(),
                    docNames.getOrDefault(e.getKey(), "Documento"),
                    e.getValue()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public DocumentoDetalleResponseDTO detalle(Long usuarioId, Long documentoId, Pageable pageable) {
    Documento documento = obtenerPorIdDeUsuario(usuarioId, documentoId);

    Page<DocumentoChunk> page =
        documentoChunkRepository.findByDocumentoIdOrderByIndiceChunkAsc(documento.getId(), pageable);

    List<DocumentoChunkResponseDTO> chunkItems =
        page.getContent().stream()
            .map(
                c ->
                    new DocumentoChunkResponseDTO(
                        c.getId(),
                        documento.getId(),
                        c.getIndiceChunk(),
                        c.getTexto(),
                        c.getPaginaOrigen(),
                        c.getTokenCount()))
            .toList();

    DocumentoChunksPageResponseDTO chunksDto =
        new DocumentoChunksPageResponseDTO(
            chunkItems, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());

    List<ResumenResponseDTO> resumenes =
        resumenRepository.findByDocumentoIdOrderByCreatedAtDesc(documento.getId()).stream()
            .filter(r -> r.getUsuario() != null && Objects.equals(r.getUsuario().getId(), usuarioId))
            .map(ResumenMapper::toResponse)
            .toList();

    List<FlashcardResponseDTO> flashcards =
        flashcardRepository.findByDocumentoId(documento.getId()).stream()
            .filter(fc -> fc.getUsuario() != null && Objects.equals(fc.getUsuario().getId(), usuarioId))
            .map(FlashcardMapper::toResponse)
            .toList();

    List<PreguntaTestResponseDTO> preguntas =
        preguntaTestRepository.findByDocumentoId(documento.getId()).stream()
            .filter(p -> p.getUsuario() != null && Objects.equals(p.getUsuario().getId(), usuarioId))
            .map(
                p ->
                    PreguntaTestMapper.toResponse(
                        p,
                        p.getId() == null
                            ? List.of()
                            : preguntaTestOpcionRepository.findByPreguntaTestIdOrderByOrdenAsc(p.getId())))
            .toList();

    return new DocumentoDetalleResponseDTO(
        DocumentoMapper.toResponse(documento), chunksDto, resumenes, flashcards, preguntas);
  }

  private List<Documento> resolveDocumentScope(Long usuarioId, Long asignaturaId, Long temaId) {
    if (usuarioId == null) {
      return List.of();
    }

    if (temaId != null) {
      Tema tema =
          temaRepository
              .findByIdAndAsignaturaUsuarioId(temaId, usuarioId)
              .orElseThrow(() -> new NotFoundException("Tema no encontrado: " + temaId));

      if (asignaturaId != null) {
        Long temaAsignaturaId = tema.getAsignatura() != null ? tema.getAsignatura().getId() : null;
        if (temaAsignaturaId == null || !asignaturaId.equals(temaAsignaturaId)) {
          throw new BadRequestException("El tema no pertenece a la asignatura indicada");
        }
      }

      return documentoRepository.findByTemaId(temaId).stream()
          .filter(d -> d.getUsuario() != null && usuarioId.equals(d.getUsuario().getId()))
          .toList();
    }

    if (asignaturaId != null) {
      if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioId).isEmpty()) {
        throw new NotFoundException("Asignatura no encontrada: " + asignaturaId);
      }
      return documentoRepository.findByAsignaturaId(asignaturaId).stream()
          .filter(d -> d.getUsuario() != null && usuarioId.equals(d.getUsuario().getId()))
          .toList();
    }

    return documentoRepository.findByUsuarioId(usuarioId);
  }

  private String buildSnippetHighlighted(String text, String query) {
    if (text == null) {
      return "";
    }
    String q = query == null ? "" : query.trim();
    if (q.isBlank()) {
      return trimToMax(text, 400);
    }

    String haystack = text;
    String lowerText = haystack.toLowerCase(Locale.ROOT);
    String lowerQ = q.toLowerCase(Locale.ROOT);
    int idx = lowerText.indexOf(lowerQ);

    String snippet;
    boolean prefix = false;
    boolean suffix = false;

    if (idx < 0) {
      snippet = trimToMax(haystack, 400);
    } else {
      int start = Math.max(0, idx - SEARCH_SNIPPET_RADIUS);
      int end = Math.min(haystack.length(), idx + q.length() + SEARCH_SNIPPET_RADIUS);
      prefix = start > 0;
      suffix = end < haystack.length();
      snippet = haystack.substring(start, end).trim();
    }

    String highlighted =
        Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE)
            .matcher(snippet)
            .replaceAll("<mark>$0</mark>");

    if (prefix) {
      highlighted = "…" + highlighted;
    }
    if (suffix) {
      highlighted = highlighted + "…";
    }
    return highlighted;
  }

  private String trimToMax(String s, int max) {
    return TextUtils.truncate(s, max);
  }
}
