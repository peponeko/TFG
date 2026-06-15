package com.easy4you.controller.api;

import com.easy4you.dto.tema.TemaRapidoRequestDTO;
import com.easy4you.dto.tema.TemaRequestDTO;
import com.easy4you.dto.tema.TemaResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.model.entity.Tema;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.TemaService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/temas")
@RequiredArgsConstructor
public class TemaController {

  private final TemaService temaService;
  private final AuthenticatedUserService authenticatedUserService;

  @GetMapping
  public ResponseEntity<List<TemaResponseDTO>> listar(
      @RequestParam(required = false) Long unidadId, @RequestParam(required = false) Long asignaturaId) {

    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();

    List<Tema> temas;
    if (asignaturaId != null) {
      temas = temaService.listarPorAsignaturaIdDeUsuario(usuarioId, asignaturaId);
    } else if (unidadId != null) {
      temas = temaService.listarPorUnidadIdDeUsuario(usuarioId, unidadId);
    } else {
      throw new BadRequestException("unidadId o asignaturaId es obligatorio");
    }

    List<TemaResponseDTO> response = temas.stream().map(this::toResponse).toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TemaResponseDTO> obtener(@PathVariable Long id) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    Tema tema = temaService.obtenerPorIdDeUsuario(usuarioId, id);
    return ResponseEntity.ok(toResponse(tema));
  }

  @PostMapping
  public ResponseEntity<TemaResponseDTO> crear(@Valid @RequestBody TemaRequestDTO request) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    Tema creado = temaService.crearDeUsuario(usuarioId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(creado));
  }

  @PostMapping("/rapido")
  public ResponseEntity<TemaResponseDTO> crearRapido(@Valid @RequestBody TemaRapidoRequestDTO request) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    Tema creado = temaService.crearRapidoDeUsuario(usuarioId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(creado));
  }

  @PutMapping("/{id}")
  public ResponseEntity<TemaResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody TemaRequestDTO request) {

    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    Tema actualizado = temaService.actualizarDeUsuario(usuarioId, id, request);
    return ResponseEntity.ok(toResponse(actualizado));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    temaService.eliminarDeUsuario(usuarioId, id);
    return ResponseEntity.noContent().build();
  }

  private TemaResponseDTO toResponse(Tema tema) {
    return new TemaResponseDTO(
        tema.getId(),
        tema.getAsignatura() != null ? tema.getAsignatura().getId() : null,
        tema.getTitulo(),
        tema.getDescripcion(),
        tema.getOrden(),
        tema.getPalabrasClave(),
        tema.getTrimestre(),
        tema.getCreatedAt(),
        tema.getUpdatedAt());
  }
}
