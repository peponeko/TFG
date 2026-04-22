package com.easy4you.controller.api;

import com.easy4you.dto.documento.DocumentoBusquedaFragmentDTO;
import com.easy4you.dto.documento.DocumentoBusquedaResultadoDTO;
import com.easy4you.dto.documento.DocumentoDetalleResponseDTO;
import com.easy4you.dto.documento.DocumentoEstadoResponseDTO;
import com.easy4you.dto.documento.DocumentoRequestDTO;
import com.easy4you.dto.documento.DocumentoChunkResponseDTO;
import com.easy4you.dto.documento.DocumentoChunksPageResponseDTO;
import com.easy4you.dto.documento.DocumentoResponseDTO;
import com.easy4you.dto.documento.DocumentoUploadResponseDTO;
import com.easy4you.dto.flashcard.FlashcardResponseDTO;
import com.easy4you.dto.pregunta.PreguntaTestOptionDTO;
import com.easy4you.dto.pregunta.PreguntaTestResponseDTO;
import com.easy4you.dto.resumen.ResumenResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.DocumentoChunk;
import com.easy4you.model.entity.Flashcard;
import com.easy4you.model.entity.PreguntaTest;
import com.easy4you.model.entity.PreguntaTestOpcion;
import com.easy4you.model.entity.Resumen;
import com.easy4you.model.entity.Tema;
import com.easy4you.model.entity.Usuario;
import com.easy4you.model.enums.EstadoProcesadoDocumento;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.DocumentoChunkRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.repository.PreguntaTestOpcionRepository;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.repository.ResumenRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.AsignaturaService;
import com.easy4you.service.DocumentoIngestionService;
import com.easy4you.service.DocumentoProcessingService;
import com.easy4you.service.DocumentoService;
import com.easy4you.service.DocumentoUploadResult;
import com.easy4you.service.TemaService;
import com.easy4you.service.UsuarioService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

  private static final int SEARCH_MAX_CHUNKS = 30;
  private static final int SEARCH_MAX_FRAGMENTS_PER_DOC = 3;
  private static final int SEARCH_SNIPPET_RADIUS = 180;

  private final DocumentoService documentoService;
  private final UsuarioService usuarioService;
  private final AsignaturaService asignaturaService;
  private final TemaService temaService;
  private final DocumentoIngestionService documentoIngestionService;
  private final DocumentoProcessingService documentoProcessingService;
  private final DocumentoRepository documentoRepository;
  private final DocumentoChunkRepository documentoChunkRepository;
  private final ResumenRepository resumenRepository;
  private final FlashcardRepository flashcardRepository;
  private final PreguntaTestRepository preguntaTestRepository;
  private final PreguntaTestOpcionRepository preguntaTestOpcionRepository;
  private final AuthenticatedUserService authenticatedUserService;
  private final ObjectMapper objectMapper;
  private final AsignaturaRepository asignaturaRepository;
  private final TemaRepository temaRepository;

  @GetMapping
  public ResponseEntity<List<DocumentoResponseDTO>> listar(
      @RequestParam(required = false) Long usuarioId, @RequestParam(required = false) Long temaId) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    List<Documento> documentos;
    if (temaId != null) {
      documentos =
          documentoService.listarPorTemaId(temaId).stream()
              .filter(d -> d.getUsuario() != null && usuarioActual.getId().equals(d.getUsuario().getId()))
              .toList();
    } else {
      documentos = documentoService.listarPorUsuarioId(usuarioActual.getId());
    }

    List<DocumentoResponseDTO> response = documentos.stream().map(this::toResponse).toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<DocumentoResponseDTO> obtener(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));
    return ResponseEntity.ok(toResponse(documento));
  }

  @GetMapping("/{id}/estado")
  public ResponseEntity<DocumentoEstadoResponseDTO> estado(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));

    long resumenes = resumenRepository.countByDocumentoId(documento.getId());
    long flashcards = flashcardRepository.countByDocumentoId(documento.getId());
    long preguntas = preguntaTestRepository.countByDocumentoId(documento.getId());

    DocumentoEstadoResponseDTO response =
        new DocumentoEstadoResponseDTO(
            documento.getId(), documento.getEstadoProcesado(), resumenes, flashcards, preguntas);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/upload")
  public ResponseEntity<DocumentoUploadResponseDTO> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("asignaturaId") Long asignaturaId,
      @RequestParam(value = "temaId", required = false) Long temaId) {

    DocumentoUploadResult result = documentoIngestionService.upload(file, asignaturaId, temaId);
    List<DocumentoResponseDTO> documentos = result.documentos().stream().map(this::toResponse).toList();

    HttpStatus status = HttpStatus.OK;
    if (result.warnings() == null || result.warnings().isEmpty()) {
      status = HttpStatus.CREATED;
    }

    return ResponseEntity.status(status).body(new DocumentoUploadResponseDTO(documentos, result.warnings()));
  }

  @GetMapping("/{id}/chunks")
  public ResponseEntity<DocumentoChunksPageResponseDTO> chunks(
      @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));

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

    DocumentoChunksPageResponseDTO response =
        new DocumentoChunksPageResponseDTO(
            items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/buscar")
  public ResponseEntity<List<DocumentoBusquedaResultadoDTO>> buscar(
      @RequestParam("q") String q,
      @RequestParam(required = false) Long asignaturaId,
      @RequestParam(required = false) Long temaId) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    String query = q == null ? "" : q.trim();
    if (query.isBlank()) {
      throw new BadRequestException("q es obligatorio");
    }

    List<Documento> documentosScope = resolveDocumentScope(usuarioActual.getId(), asignaturaId, temaId);
    List<Long> documentoIds = documentosScope.stream().map(Documento::getId).filter(Objects::nonNull).toList();
    if (documentoIds.isEmpty()) {
      return ResponseEntity.ok(List.of());
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
      page = documentoChunkRepository.findByDocumentoIdInAndTextoContainingIgnoreCase(
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

    List<DocumentoBusquedaResultadoDTO> response =
        grouped.entrySet().stream()
            .map(
                e ->
                    new DocumentoBusquedaResultadoDTO(
                        e.getKey(),
                        docNames.getOrDefault(e.getKey(), "Documento"),
                        e.getValue()))
            .toList();

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}/detalle")
  public ResponseEntity<DocumentoDetalleResponseDTO> detalle(
      @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));

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
            .filter(r -> r.getUsuario() != null && Objects.equals(r.getUsuario().getId(), usuarioActual.getId()))
            .map(this::toResumenResponse)
            .toList();

    List<FlashcardResponseDTO> flashcards =
        flashcardRepository.findByDocumentoId(documento.getId()).stream()
            .filter(fc -> fc.getUsuario() != null && Objects.equals(fc.getUsuario().getId(), usuarioActual.getId()))
            .map(this::toFlashcardResponse)
            .toList();

    List<PreguntaTestResponseDTO> preguntas =
        preguntaTestRepository.findByDocumentoId(documento.getId()).stream()
            .filter(p -> p.getUsuario() != null && Objects.equals(p.getUsuario().getId(), usuarioActual.getId()))
            .map(this::toPreguntaTestResponse)
            .toList();

    DocumentoDetalleResponseDTO response =
        new DocumentoDetalleResponseDTO(toResponse(documento), chunksDto, resumenes, flashcards, preguntas);

    return ResponseEntity.ok(response);
  }

  @PostMapping
  public ResponseEntity<DocumentoResponseDTO> crear(@Valid @RequestBody DocumentoRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    if (!usuarioActual.getId().equals(request.getUsuarioId())) {
      throw new BadRequestException("usuarioId no coincide con el usuario autenticado");
    }

    Usuario usuario = usuarioService.obtenerPorId(request.getUsuarioId());
    Asignatura asignatura = asignaturaService.obtenerPorId(request.getAsignaturaId());

    Tema tema = null;
    if (request.getTemaId() != null) {
      tema = temaService.obtenerPorId(request.getTemaId());
    }

    Documento documento = new Documento();
    documento.setUsuario(usuario);
    documento.setAsignatura(asignatura);
    documento.setTema(tema);
    documento.setNombreOriginal(request.getNombreOriginal());
    documento.setRutaArchivo(request.getRutaArchivo());
    documento.setMimeType(request.getMimeType());
    documento.setExtension(request.getExtension());
    documento.setTamanoBytes(request.getTamanoBytes());
    documento.setChecksumSha256(request.getChecksumSha256());
    documento.setPaginas(request.getPaginas());

    Documento creado = documentoService.crear(documento);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(creado));
  }

  @PutMapping("/{id}")
  public ResponseEntity<DocumentoResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody DocumentoRequestDTO request) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Documento existente =
        documentoRepository
            .findByIdAndUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));

    Documento datos = new Documento();
    datos.setNombreOriginal(request.getNombreOriginal());
    datos.setRutaArchivo(request.getRutaArchivo());
    datos.setMimeType(request.getMimeType());
    datos.setExtension(request.getExtension());
    datos.setTamanoBytes(request.getTamanoBytes());
    datos.setChecksumSha256(request.getChecksumSha256());
    datos.setPaginas(request.getPaginas());
    datos.setTextoExtraido(existente.getTextoExtraido());
    datos.setEstadoProcesado(existente.getEstadoProcesado());
    datos.setErrorExtraccion(existente.getErrorExtraccion());

    if (request.getTemaId() != null) {
      datos.setTema(temaService.obtenerPorId(request.getTemaId()));
    } else {
      datos.setTema(existente.getTema());
    }

    Documento actualizado = documentoService.actualizar(id, datos);
    return ResponseEntity.ok(toResponse(actualizado));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    if (documentoRepository.findByIdAndUsuarioId(id, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Documento no encontrado: " + id);
    }
    documentoService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/reprocesar")
  public ResponseEntity<Map<String, String>> reprocesar(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));

    documentoProcessingService.procesarAsync(documento.getId());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "processing"));
  }

  private DocumentoResponseDTO toResponse(Documento documento) {
    EstadoProcesadoDocumento estadoProcesado = documento.getEstadoProcesado();
    if (estadoProcesado == EstadoProcesadoDocumento.LISTO) {
      estadoProcesado = EstadoProcesadoDocumento.PROCESADO;
    }

    return new DocumentoResponseDTO(
        documento.getId(),
        documento.getUsuario() != null ? documento.getUsuario().getId() : null,
        documento.getAsignatura() != null ? documento.getAsignatura().getId() : null,
        documento.getTema() != null ? documento.getTema().getId() : null,
        documento.getNombreOriginal(),
        documento.getRutaArchivo(),
        documento.getMimeType(),
        documento.getExtension(),
        documento.getTamanoBytes(),
        documento.getChecksumSha256(),
        documento.getPaginas(),
        estadoProcesado,
        documento.getErrorExtraccion(),
        documento.getCreatedAt(),
        documento.getUpdatedAt());
  }

  private ResumenResponseDTO toResumenResponse(Resumen resumen) {
    List<String> puntosClave = parseStringList(resumen.getPuntosClaveJson());

    return new ResumenResponseDTO(
        resumen.getId(),
        resumen.getUsuario() != null ? resumen.getUsuario().getId() : null,
        resumen.getTema() != null ? resumen.getTema().getId() : null,
        resumen.getDocumento() != null ? resumen.getDocumento().getId() : null,
        resumen.getTitulo(),
        resumen.getContenido(),
        puntosClave,
        resumen.getOrigen(),
        resumen.getCreatedAt(),
        resumen.getUpdatedAt());
  }

  private FlashcardResponseDTO toFlashcardResponse(Flashcard fc) {
    return new FlashcardResponseDTO(
        fc.getId(),
        fc.getUsuario() != null ? fc.getUsuario().getId() : null,
        fc.getTema() != null ? fc.getTema().getId() : null,
        fc.getDocumento() != null ? fc.getDocumento().getId() : null,
        fc.getChunkOrigen() != null ? fc.getChunkOrigen().getId() : null,
        fc.getPregunta(),
        fc.getRespuesta(),
        fc.getDificultad(),
        fc.getCreatedAt(),
        fc.getUpdatedAt());
  }

  private PreguntaTestResponseDTO toPreguntaTestResponse(PreguntaTest p) {
    List<PreguntaTestOpcion> opciones =
        p.getId() == null
            ? List.of()
            : preguntaTestOpcionRepository.findByPreguntaTestIdOrderByOrdenAsc(p.getId());

    List<PreguntaTestOptionDTO> opcionesDto =
        opciones.stream().map(o -> new PreguntaTestOptionDTO(o.getId(), o.getTexto(), o.getOrden())).toList();

    return new PreguntaTestResponseDTO(
        p.getId(),
        p.getUsuario() != null ? p.getUsuario().getId() : null,
        p.getTema() != null ? p.getTema().getId() : null,
        p.getDocumento() != null ? p.getDocumento().getId() : null,
        p.getChunkOrigen() != null ? p.getChunkOrigen().getId() : null,
        p.getEnunciado(),
        p.getExplicacion(),
        p.getDificultad(),
        opcionesDto,
        p.getCreatedAt(),
        p.getUpdatedAt());
  }

  private List<String> parseStringList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
      if (list == null) {
        return List.of();
      }
      return list.stream().filter(Objects::nonNull).toList();
    } catch (Exception ex) {
      return List.of();
    }
  }

  private List<Documento> resolveDocumentScope(Long usuarioId, Long asignaturaId, Long temaId) {
    if (usuarioId == null) {
      return List.of();
    }

    if (temaId != null) {
      Tema tema =
          temaRepository
              .findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(temaId, usuarioId)
              .orElseThrow(() -> new NotFoundException("Tema no encontrado: " + temaId));

      if (asignaturaId != null) {
        Long temaAsignaturaId =
            tema.getUnidad() != null
                    && tema.getUnidad().getResultadoAprendizaje() != null
                    && tema.getUnidad().getResultadoAprendizaje().getAsignatura() != null
                ? tema.getUnidad().getResultadoAprendizaje().getAsignatura().getId()
                : null;
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
    if (s == null) {
      return "";
    }
    String t = s.trim();
    if (t.length() <= max) {
      return t;
    }
    return t.substring(0, Math.max(0, max - 1)).trim() + "…";
  }
}
