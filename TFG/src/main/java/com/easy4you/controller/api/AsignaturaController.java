package com.easy4you.controller.api;

import com.easy4you.dto.asignatura.AsignaturaRequestDTO;
import com.easy4you.dto.asignatura.AsignaturaResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.AsignaturaService;
import com.easy4you.service.UsuarioService;
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
@RequestMapping("/api/asignaturas")
@RequiredArgsConstructor
public class AsignaturaController {

  private final AsignaturaService asignaturaService;
  private final UsuarioService usuarioService;
  private final AuthenticatedUserService authenticatedUserService;
  private final AsignaturaRepository asignaturaRepository;

  @GetMapping
  public ResponseEntity<List<AsignaturaResponseDTO>> listar(@RequestParam(required = false) Long usuarioId) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    if (usuarioId != null && !usuarioId.equals(usuarioActual.getId())) {
      throw new BadRequestException("usuarioId no coincide con el usuario autenticado");
    }

    List<Asignatura> asignaturas = asignaturaService.listarPorUsuarioId(usuarioActual.getId());

    List<AsignaturaResponseDTO> response = asignaturas.stream().map(this::toResponse).toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<AsignaturaResponseDTO> obtener(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Asignatura asignatura =
        asignaturaRepository
            .findByIdAndUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + id));

    return ResponseEntity.ok(toResponse(asignatura));
  }

  @PostMapping
  public ResponseEntity<AsignaturaResponseDTO> crear(@Valid @RequestBody AsignaturaRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    if (request.getUsuarioId() == null || !request.getUsuarioId().equals(usuarioActual.getId())) {
      throw new BadRequestException("usuarioId no coincide con el usuario autenticado");
    }

    Usuario usuario = usuarioService.obtenerPorId(usuarioActual.getId());

    Asignatura asignatura = new Asignatura();
    asignatura.setUsuario(usuario);
    asignatura.setNombre(request.getNombre());
    asignatura.setDescripcion(request.getDescripcion());
    asignatura.setColorHex(request.getColorHex());

    Asignatura creada = asignaturaService.crear(asignatura);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(creada));
  }

  @PutMapping("/{id}")
  public ResponseEntity<AsignaturaResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody AsignaturaRequestDTO request) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Asignatura existente =
        asignaturaRepository
            .findByIdAndUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + id));

    Asignatura datos = new Asignatura();
    datos.setNombre(request.getNombre());
    datos.setDescripcion(request.getDescripcion());
    datos.setColorHex(request.getColorHex());
    datos.setUsuario(existente.getUsuario());

    Asignatura actualizada = asignaturaService.actualizar(id, datos);
    return ResponseEntity.ok(toResponse(actualizada));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    if (asignaturaRepository.findByIdAndUsuarioId(id, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Asignatura no encontrada: " + id);
    }

    asignaturaService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  private AsignaturaResponseDTO toResponse(Asignatura asignatura) {
    return new AsignaturaResponseDTO(
        asignatura.getId(),
        asignatura.getUsuario() != null ? asignatura.getUsuario().getId() : null,
        asignatura.getNombre(),
        asignatura.getDescripcion(),
        asignatura.getColorHex(),
        asignatura.getCreatedAt(),
        asignatura.getUpdatedAt());
  }
}
