package com.easy4you.controller.api;

import com.easy4you.dto.pregunta.PreguntaTestResponseDTO;
import com.easy4you.dto.pregunta.ResponderPreguntaTestRequestDTO;
import com.easy4you.dto.pregunta.ResponderPreguntaTestResponseDTO;
import com.easy4you.model.entity.Usuario;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.PreguntaTestGenerationService;
import com.easy4you.service.PreguntaTestService;
import jakarta.validation.Valid;
import java.util.List;
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
  private final PreguntaTestService preguntaTestService;

  @PostMapping("/generar/{documentoId}")
  public ResponseEntity<Map<String, String>> generar(@PathVariable Long documentoId) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    preguntaTestGenerationService.solicitarGeneracionParaDocumento(usuarioActual.getId(), documentoId);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "processing"));
  }

  @GetMapping("/documento/{documentoId}")
  public ResponseEntity<List<PreguntaTestResponseDTO>> listarPorDocumento(@PathVariable Long documentoId) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    return ResponseEntity.ok(preguntaTestService.listarPorDocumento(usuarioId, documentoId));
  }

  @GetMapping("/tema/{temaId}")
  public ResponseEntity<List<PreguntaTestResponseDTO>> listarPorTema(@PathVariable Long temaId) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    return ResponseEntity.ok(preguntaTestService.listarPorTema(usuarioId, temaId));
  }

  @PostMapping("/{id}/responder")
  public ResponseEntity<ResponderPreguntaTestResponseDTO> responder(
      @PathVariable Long id, @Valid @RequestBody ResponderPreguntaTestRequestDTO request) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    return ResponseEntity.ok(preguntaTestService.responder(usuarioId, id, request));
  }
}
