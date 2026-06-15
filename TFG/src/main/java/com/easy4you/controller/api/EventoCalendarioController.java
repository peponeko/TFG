package com.easy4you.controller.api;

import com.easy4you.dto.evento.EventoCalendarioRequestDTO;
import com.easy4you.dto.evento.EventoCalendarioResponseDTO;
import com.easy4you.model.entity.EventoCalendario;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.EventoCalendarioService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoCalendarioController {

  private final EventoCalendarioService eventoCalendarioService;
  private final AuthenticatedUserService authenticatedUserService;

  @Transactional(readOnly = true)
  @GetMapping
  public ResponseEntity<List<EventoCalendarioResponseDTO>> listar(
      @RequestParam(required = false) LocalDate desde, @RequestParam(required = false) LocalDate hasta) {

    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    List<EventoCalendarioResponseDTO> response =
        eventoCalendarioService.listar(usuarioId, desde, hasta).stream().map(this::toResponse).toList();
    return ResponseEntity.ok(response);
  }

  @Transactional(readOnly = true)
  @GetMapping("/{id}")
  public ResponseEntity<EventoCalendarioResponseDTO> obtener(@PathVariable Long id) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    EventoCalendario evento = eventoCalendarioService.obtenerPorIdDeUsuario(usuarioId, id);
    return ResponseEntity.ok(toResponse(evento));
  }

  @PostMapping
  public ResponseEntity<EventoCalendarioResponseDTO> crear(@Valid @RequestBody EventoCalendarioRequestDTO request) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    EventoCalendario creado = eventoCalendarioService.crearDeUsuario(usuarioId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(creado));
  }

  @PutMapping("/{id}")
  public ResponseEntity<EventoCalendarioResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody EventoCalendarioRequestDTO request) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    EventoCalendario actualizado = eventoCalendarioService.actualizarDeUsuario(usuarioId, id, request);
    return ResponseEntity.ok(toResponse(actualizado));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    eventoCalendarioService.eliminarDeUsuario(usuarioId, id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/completar")
  public ResponseEntity<EventoCalendarioResponseDTO> toggleCompletado(@PathVariable Long id) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    EventoCalendario actualizado = eventoCalendarioService.toggleCompletado(usuarioId, id);
    return ResponseEntity.ok(toResponse(actualizado));
  }

  private EventoCalendarioResponseDTO toResponse(EventoCalendario e) {
    return new EventoCalendarioResponseDTO(
        e.getId(),
        e.getUsuario() != null ? e.getUsuario().getId() : null,
        e.getAsignatura() != null ? e.getAsignatura().getId() : null,
        e.getTitulo(),
        e.getDescripcion(),
        e.getFechaInicio(),
        e.getHoraInicio(),
        e.getTipo(),
        e.isCompletado(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }
}

