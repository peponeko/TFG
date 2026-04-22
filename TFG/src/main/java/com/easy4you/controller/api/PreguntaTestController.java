package com.easy4you.controller.api;

import com.easy4you.dto.pregunta.PreguntaTestOptionDTO;
import com.easy4you.dto.pregunta.PreguntaTestResponseDTO;
import com.easy4you.dto.pregunta.ResponderPreguntaTestRequestDTO;
import com.easy4you.dto.pregunta.ResponderPreguntaTestResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.PreguntaTest;
import com.easy4you.model.entity.PreguntaTestOpcion;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.PreguntaTestOpcionRepository;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.PreguntaTestGenerationService;
import com.easy4you.service.ProgresoUsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preguntas")
@RequiredArgsConstructor
public class PreguntaTestController {

  private final AuthenticatedUserService authenticatedUserService;
  private final PreguntaTestGenerationService preguntaTestGenerationService;
  private final PreguntaTestRepository preguntaTestRepository;
  private final PreguntaTestOpcionRepository preguntaTestOpcionRepository;
  private final DocumentoRepository documentoRepository;
  private final TemaRepository temaRepository;
  private final ProgresoUsuarioService progresoUsuarioService;

  @PostMapping("/generar/{documentoId}")
  public ResponseEntity<Map<String, String>> generar(@PathVariable Long documentoId) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    preguntaTestGenerationService.solicitarGeneracionParaDocumento(usuarioActual.getId(), documentoId);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "processing"));
  }

  @GetMapping("/documento/{documentoId}")
  public ResponseEntity<List<PreguntaTestResponseDTO>> listarPorDocumento(@PathVariable Long documentoId) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(documentoId, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + documentoId));

    List<PreguntaTestResponseDTO> response =
        preguntaTestRepository.findByDocumentoId(documento.getId()).stream()
            .filter(p -> p.getUsuario() != null && Objects.equals(p.getUsuario().getId(), usuarioActual.getId()))
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/tema/{temaId}")
  public ResponseEntity<List<PreguntaTestResponseDTO>> listarPorTema(@PathVariable Long temaId) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    if (temaRepository.findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(temaId, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Tema no encontrado: " + temaId);
    }

    List<PreguntaTestResponseDTO> response =
        preguntaTestRepository.findByTemaId(temaId).stream()
            .filter(p -> p.getUsuario() != null && Objects.equals(p.getUsuario().getId(), usuarioActual.getId()))
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(response);
  }

  @PostMapping("/{id}/responder")
  public ResponseEntity<ResponderPreguntaTestResponseDTO> responder(
      @PathVariable Long id, @Valid @RequestBody ResponderPreguntaTestRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    PreguntaTest pregunta =
        preguntaTestRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Pregunta test no encontrada: " + id));

    if (pregunta.getUsuario() == null || !usuarioActual.getId().equals(pregunta.getUsuario().getId())) {
      throw new NotFoundException("Pregunta test no encontrada: " + id);
    }

    List<PreguntaTestOpcion> opciones =
        preguntaTestOpcionRepository.findByPreguntaTestIdOrderByOrdenAsc(pregunta.getId());
    if (opciones.size() < 4) {
      throw new BadRequestException("La pregunta no tiene opciones suficientes");
    }

    int indiceCorrecto = -1;
    for (int i = 0; i < opciones.size(); i++) {
      if (opciones.get(i).isEsCorrecta()) {
        indiceCorrecto = i;
        break;
      }
    }

    if (indiceCorrecto < 0) {
      throw new BadRequestException("La pregunta no tiene respuesta correcta configurada");
    }

    boolean correcta = Objects.equals(indiceCorrecto, request.getIndiceOpcion());

    progresoUsuarioService.registrarRespuestaTest(usuarioActual.getId(), pregunta.getId(), correcta);

    return ResponseEntity.ok(
        new ResponderPreguntaTestResponseDTO(correcta, indiceCorrecto, pregunta.getExplicacion()));
  }

  private PreguntaTestResponseDTO toResponse(PreguntaTest p) {
    List<PreguntaTestOpcion> opciones =
        p.getId() == null
            ? List.of()
            : preguntaTestOpcionRepository.findByPreguntaTestIdOrderByOrdenAsc(p.getId());

    List<PreguntaTestOptionDTO> opcionesDto =
        opciones.stream()
            .map(o -> new PreguntaTestOptionDTO(o.getId(), o.getTexto(), o.getOrden()))
            .toList();

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
}
