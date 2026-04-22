package com.easy4you.controller.api;

import com.easy4you.dto.flashcard.FlashcardResponseDTO;
import com.easy4you.dto.flashcard.FlashcardUpdateRequestDTO;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.Flashcard;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.FlashcardGenerationService;
import com.easy4you.service.FlashcardService;
import com.easy4you.service.ProgresoUsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

  private final AuthenticatedUserService authenticatedUserService;
  private final FlashcardGenerationService flashcardGenerationService;
  private final FlashcardService flashcardService;
  private final FlashcardRepository flashcardRepository;
  private final DocumentoRepository documentoRepository;
  private final TemaRepository temaRepository;
  private final ProgresoUsuarioService progresoUsuarioService;

  @PostMapping("/generar/{documentoId}")
  public ResponseEntity<Map<String, String>> generar(@PathVariable Long documentoId) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    flashcardGenerationService.solicitarGeneracionParaDocumento(usuarioActual.getId(), documentoId);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "processing"));
  }

  @GetMapping("/documento/{documentoId}")
  public ResponseEntity<List<FlashcardResponseDTO>> listarPorDocumento(@PathVariable Long documentoId) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(documentoId, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + documentoId));

    List<FlashcardResponseDTO> response =
        flashcardRepository.findByDocumentoId(documento.getId()).stream()
            .filter(fc -> fc.getUsuario() != null && Objects.equals(fc.getUsuario().getId(), usuarioActual.getId()))
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/tema/{temaId}")
  public ResponseEntity<List<FlashcardResponseDTO>> listarPorTema(@PathVariable Long temaId) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    if (temaRepository.findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(temaId, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Tema no encontrado: " + temaId);
    }

    List<FlashcardResponseDTO> response =
        flashcardRepository.findByTemaId(temaId).stream()
            .filter(fc -> fc.getUsuario() != null && Objects.equals(fc.getUsuario().getId(), usuarioActual.getId()))
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  public ResponseEntity<FlashcardResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody FlashcardUpdateRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    Flashcard existente = flashcardService.obtenerPorId(id);
    if (existente.getUsuario() == null || !usuarioActual.getId().equals(existente.getUsuario().getId())) {
      throw new NotFoundException("Flashcard no encontrada: " + id);
    }

    Flashcard datos = new Flashcard();
    datos.setPregunta(request.getPregunta());
    datos.setRespuesta(request.getRespuesta());
    datos.setDificultad(request.getDificultad());

    Flashcard updated = flashcardService.actualizar(id, datos);
    return ResponseEntity.ok(toResponse(updated));
  }

  @PostMapping("/{id}/repasar")
  public ResponseEntity<Void> repasar(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    progresoUsuarioService.registrarRepasoFlashcard(usuarioActual.getId(), id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    Flashcard existente = flashcardService.obtenerPorId(id);
    if (existente.getUsuario() == null || !usuarioActual.getId().equals(existente.getUsuario().getId())) {
      throw new NotFoundException("Flashcard no encontrada: " + id);
    }

    flashcardService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  private FlashcardResponseDTO toResponse(Flashcard fc) {
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
}
