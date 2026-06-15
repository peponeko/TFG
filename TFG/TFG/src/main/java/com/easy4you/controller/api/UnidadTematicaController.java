package com.easy4you.controller.api;

import com.easy4you.dto.unidadtematica.UnidadTematicaRequestDTO;
import com.easy4you.dto.unidadtematica.UnidadTematicaResponseDTO;
import com.easy4you.model.entity.UnidadTematica;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.UnidadTematicaService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/api/unidades-tematicas")
@RequiredArgsConstructor
public class UnidadTematicaController {

  private final UnidadTematicaService unidadTematicaService;
  private final AuthenticatedUserService authenticatedUserService;

  @Transactional(readOnly = true)
  @GetMapping
  public ResponseEntity<List<UnidadTematicaResponseDTO>> listar(@RequestParam Long asignaturaId) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    List<UnidadTematicaResponseDTO> response =
        unidadTematicaService.listarPorAsignaturaDeUsuario(usuarioId, asignaturaId).stream()
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(response);
  }

  @Transactional(readOnly = true)
  @GetMapping("/{id}")
  public ResponseEntity<UnidadTematicaResponseDTO> obtener(@PathVariable Long id) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    UnidadTematica u = unidadTematicaService.obtenerPorIdDeUsuario(usuarioId, id);
    return ResponseEntity.ok(toResponse(u));
  }

  @PostMapping
  public ResponseEntity<UnidadTematicaResponseDTO> crear(@Valid @RequestBody UnidadTematicaRequestDTO request) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    UnidadTematica creada = unidadTematicaService.crearDeUsuario(usuarioId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(creada));
  }

  @PutMapping("/{id}")
  public ResponseEntity<UnidadTematicaResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody UnidadTematicaRequestDTO request) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    UnidadTematica actualizada = unidadTematicaService.actualizarDeUsuario(usuarioId, id, request);
    return ResponseEntity.ok(toResponse(actualizada));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    unidadTematicaService.eliminarDeUsuario(usuarioId, id);
    return ResponseEntity.noContent().build();
  }

  private UnidadTematicaResponseDTO toResponse(UnidadTematica u) {
    return new UnidadTematicaResponseDTO(
        u.getId(),
        u.getAsignatura() != null ? u.getAsignatura().getId() : null,
        u.getTitulo(),
        u.getOrden(),
        u.getTrimestre(),
        u.getCreatedAt(),
        u.getUpdatedAt());
  }
}

