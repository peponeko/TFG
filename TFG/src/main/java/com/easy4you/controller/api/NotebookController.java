package com.easy4you.controller.api;

import com.easy4you.dto.asignatura.AsignaturaResponseDTO;
import com.easy4you.dto.documento.DocumentoResponseDTO;
import com.easy4you.dto.flashcard.FlashcardResponseDTO;
import com.easy4you.dto.nota.NotaResponseDTO;
import com.easy4you.dto.notebook.NotebookCreateRequestDTO;
import com.easy4you.dto.notebook.NotebookCompartidoResponseDTO;
import com.easy4you.dto.notebook.NotebookCompartirRequestDTO;
import com.easy4you.dto.notebook.NotebookOverviewResponseDTO;
import com.easy4you.dto.pregunta.PreguntaTestOptionDTO;
import com.easy4you.dto.pregunta.PreguntaTestResponseDTO;
import com.easy4you.dto.resumen.ResumenResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.enums.RolNotebookCompartido;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.Flashcard;
import com.easy4you.model.entity.Nota;
import com.easy4you.model.entity.NotebookCompartido;
import com.easy4you.model.entity.PreguntaTest;
import com.easy4you.model.entity.PreguntaTestOpcion;
import com.easy4you.model.entity.Resumen;
import com.easy4you.model.entity.Usuario;
import com.easy4you.model.enums.EstadoProcesadoDocumento;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.repository.NotaRepository;
import com.easy4you.repository.PreguntaTestOpcionRepository;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.repository.ResumenRepository;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.NotebookCompartidoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/notebooks")
@RequiredArgsConstructor
public class NotebookController {

  private final AuthenticatedUserService authenticatedUserService;
  private final AsignaturaRepository asignaturaRepository;
  private final DocumentoRepository documentoRepository;
  private final ResumenRepository resumenRepository;
  private final FlashcardRepository flashcardRepository;
  private final PreguntaTestRepository preguntaTestRepository;
  private final PreguntaTestOpcionRepository preguntaTestOpcionRepository;
  private final NotaRepository notaRepository;
  private final NotebookCompartidoService notebookCompartidoService;
  private final ObjectMapper objectMapper;

  @GetMapping
  public ResponseEntity<List<AsignaturaResponseDTO>> listar() {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    List<AsignaturaResponseDTO> response =
        asignaturaRepository.findByUsuarioIdOrderByNombreAsc(usuarioActual.getId()).stream()
            .map(this::toAsignaturaResponse)
            .toList();
    return ResponseEntity.ok(response);
  }

  @PostMapping("/compartir")
  public ResponseEntity<NotebookCompartidoResponseDTO> compartir(
      @Valid @RequestBody NotebookCompartirRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    NotebookCompartido saved =
        notebookCompartidoService.compartir(
            usuarioActual.getId(),
            request.getAsignaturaId(),
            request.getUsuarioInvitadoId(),
            request.getRol());

    return ResponseEntity.status(HttpStatus.CREATED).body(toCompartidoResponse(saved));
  }

  @GetMapping("/compartidos-conmigo")
  public ResponseEntity<List<NotebookCompartidoResponseDTO>> compartidosConmigo() {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    List<NotebookCompartidoResponseDTO> response =
        notebookCompartidoService.listarCompartidosConmigo(usuarioActual.getId()).stream()
            .map(this::toCompartidoResponse)
            .toList();
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}/revocar/{usuarioId}")
  public ResponseEntity<Void> revocar(@PathVariable Long id, @PathVariable Long usuarioId) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    notebookCompartidoService.revocar(usuarioActual.getId(), id, usuarioId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping
  public ResponseEntity<AsignaturaResponseDTO> crear(@Valid @RequestBody NotebookCreateRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    Asignatura asignatura = new Asignatura();
    asignatura.setUsuario(usuarioActual);
    asignatura.setNombre(request.getNombre());
    asignatura.setDescripcion(request.getDescripcion());
    asignatura.setColorHex(request.getColorHex());

    Asignatura created = asignaturaRepository.save(asignatura);
    return ResponseEntity.status(HttpStatus.CREATED).body(toAsignaturaResponse(created));
  }

  @GetMapping("/{id}/overview")
  public ResponseEntity<NotebookOverviewResponseDTO> overview(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    // Verificar acceso (propietario o invitado)
    if (!notebookCompartidoService.tieneAcceso(usuarioActual.getId(), id)) {
      throw new NotFoundException("Notebook no encontrado o sin acceso: " + id);
    }

    // Obtener rol del usuario
    RolNotebookCompartido rol = notebookCompartidoService.obtenerRol(usuarioActual.getId(), id)
        .orElse(RolNotebookCompartido.VIEWER);

    // Obtener asignatura (puede ser propietario o compartida)
    Asignatura asignatura = asignaturaRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Notebook no encontrado: " + id));

    // Todos los documentos de la asignatura (el usuario tiene acceso)
    List<Documento> documentos = documentoRepository.findByAsignaturaId(asignatura.getId());

    List<Long> documentoIds = documentos.stream().map(Documento::getId).filter(Objects::nonNull).toList();

    // Resúmenes de los documentos
    List<ResumenResponseDTO> resumenes = documentoIds.isEmpty()
        ? List.of()
        : resumenRepository.findByDocumentoIdInOrderByCreatedAtDesc(documentoIds).stream()
            .map(this::toResumenResponse)
            .toList();

    // Flashcards de los documentos
    List<FlashcardResponseDTO> flashcards = documentoIds.isEmpty()
        ? List.of()
        : flashcardRepository.findByDocumentoIdIn(documentoIds).stream()
            .map(this::toFlashcardResponse)
            .toList();

    // Preguntas de los documentos
    List<PreguntaTestResponseDTO> preguntas = documentoIds.isEmpty()
        ? List.of()
        : preguntaTestRepository.findByDocumentoIdIn(documentoIds).stream()
            .map(this::toPreguntaTestResponse)
            .toList();

    // Notas del usuario en esta asignatura
    List<NotaResponseDTO> notas = notaRepository.search(usuarioActual.getId(), null, null, asignatura.getId()).stream()
        .map(this::toNotaResponse)
        .toList();

    List<DocumentoResponseDTO> documentosDto = documentos.stream().map(this::toDocumentoResponse).toList();

    NotebookOverviewResponseDTO response = new NotebookOverviewResponseDTO(
        toAsignaturaResponse(asignatura), documentosDto, resumenes, flashcards, preguntas, notas);
    return ResponseEntity.ok(response);
  }

  private AsignaturaResponseDTO toAsignaturaResponse(Asignatura asignatura) {
    return new AsignaturaResponseDTO(
        asignatura.getId(),
        asignatura.getUsuario() != null ? asignatura.getUsuario().getId() : null,
        asignatura.getNombre(),
        asignatura.getDescripcion(),
        asignatura.getColorHex(),
        asignatura.getTrimestre(),
        asignatura.getCreatedAt(),
        asignatura.getUpdatedAt());
  }

  private DocumentoResponseDTO toDocumentoResponse(Documento documento) {
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

  private NotaResponseDTO toNotaResponse(Nota nota) {
    return new NotaResponseDTO(
        nota.getId(),
        nota.getUsuario() != null ? nota.getUsuario().getId() : null,
        nota.getDocumento() != null ? nota.getDocumento().getId() : null,
        nota.getChunk() != null ? nota.getChunk().getId() : null,
        nota.getTema() != null ? nota.getTema().getId() : null,
        nota.getTitulo(),
        nota.getContenido(),
        nota.getColorHex(),
        nota.getCreatedAt(),
        nota.getUpdatedAt());
  }

  private NotebookCompartidoResponseDTO toCompartidoResponse(NotebookCompartido nc) {
    return new NotebookCompartidoResponseDTO(
        nc.getId(),
        nc.getAsignatura() != null ? nc.getAsignatura().getId() : null,
        nc.getPropietario() != null ? nc.getPropietario().getId() : null,
        nc.getUsuarioInvitado() != null ? nc.getUsuarioInvitado().getId() : null,
        nc.getRol(),
        nc.getCreatedAt(),
        nc.getUpdatedAt());
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
}
