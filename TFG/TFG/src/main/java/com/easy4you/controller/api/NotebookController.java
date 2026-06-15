package com.easy4you.controller.api;

import com.easy4you.dto.asignatura.AsignaturaResponseDTO;
import com.easy4you.dto.notebook.NotebookCreateRequestDTO;
import com.easy4you.dto.notebook.NotebookCompartidoResponseDTO;
import com.easy4you.dto.notebook.NotebookCompartirRequestDTO;
import com.easy4you.dto.notebook.NotebookOverviewResponseDTO;
import com.easy4you.mapper.AsignaturaMapper;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.NotebookCompartido;
import com.easy4you.model.entity.Usuario;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.AsignaturaService;
import com.easy4you.service.NotebookCompartidoService;
import com.easy4you.service.NotebookService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/notebooks")
@RequiredArgsConstructor
public class NotebookController {

  private final AuthenticatedUserService authenticatedUserService;
  private final AsignaturaService asignaturaService;
  private final NotebookCompartidoService notebookCompartidoService;
  private final NotebookService notebookService;

  @GetMapping
  public ResponseEntity<List<AsignaturaResponseDTO>> listar() {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    List<AsignaturaResponseDTO> response =
        asignaturaService.listarPorUsuarioId(usuarioActual.getId()).stream()
            .map(AsignaturaMapper::toResponse)
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

    Asignatura created = asignaturaService.crear(asignatura);
    return ResponseEntity.status(HttpStatus.CREATED).body(AsignaturaMapper.toResponse(created));
  }

  @GetMapping("/{id}/overview")
  public ResponseEntity<NotebookOverviewResponseDTO> overview(
      @PathVariable Long id, @RequestParam(value = "temaId", required = false) Long temaId) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    return ResponseEntity.ok(notebookService.obtenerOverview(usuarioId, id, temaId));
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
}
