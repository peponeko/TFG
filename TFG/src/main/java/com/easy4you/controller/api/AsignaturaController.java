package com.easy4you.controller.api;

import com.easy4you.dto.asignatura.AsignaturaRequestDTO;
import com.easy4you.dto.asignatura.AsignaturaResponseDTO;
import com.easy4you.dto.tema.TemaPlanoResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.mapper.AsignaturaMapper;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Usuario;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.AsignaturaService;
import com.easy4you.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/asignaturas")
@RequiredArgsConstructor
public class AsignaturaController {

  private final AsignaturaService asignaturaService;
  private final UsuarioService usuarioService;
  private final AuthenticatedUserService authenticatedUserService;

  @Transactional(readOnly = true)
  @GetMapping
  public ResponseEntity<List<AsignaturaResponseDTO>> listar(
      @RequestParam(required = false) Long usuarioId, @RequestParam(required = false) Integer trimestre) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    if (usuarioId != null && !usuarioId.equals(usuarioActual.getId())) {
      throw new BadRequestException("usuarioId no coincide con el usuario autenticado");
    }

    List<Asignatura> asignaturas;
    if (trimestre == null) {
      asignaturas = new ArrayList<>(asignaturaService.listarPorUsuarioId(usuarioActual.getId()));
      asignaturas.sort(
          Comparator.comparing(
                  Asignatura::getTrimestre, Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(a -> a.getNombre() == null ? "" : a.getNombre(), String.CASE_INSENSITIVE_ORDER));
    } else {
      asignaturas = asignaturaService.listarPorUsuarioIdYTrimestre(usuarioActual.getId(), trimestre);
    }

    List<AsignaturaResponseDTO> response =
        asignaturas.stream().map(AsignaturaMapper::toResponse).toList();
    return ResponseEntity.ok(response);
  }

  @Transactional(readOnly = true)
  @GetMapping("/{id}")
  public ResponseEntity<AsignaturaResponseDTO> obtener(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Asignatura asignatura = asignaturaService.obtenerPorIdDeUsuario(usuarioActual.getId(), id);

    return ResponseEntity.ok(AsignaturaMapper.toResponse(asignatura));
  }

  @PostMapping
  public ResponseEntity<AsignaturaResponseDTO> crear(@Valid @RequestBody AsignaturaRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    if (request.getUsuarioId() != null && !request.getUsuarioId().equals(usuarioActual.getId())) {
      throw new BadRequestException("usuarioId no coincide con el usuario autenticado");
    }

    Usuario usuario = usuarioService.obtenerPorId(usuarioActual.getId());

    Asignatura asignatura = new Asignatura();
    asignatura.setUsuario(usuario);
    asignatura.setNombre(request.getNombre());
    asignatura.setDescripcion(request.getDescripcion());
    asignatura.setColorHex(request.getColorHex());
    asignatura.setTrimestre(request.getTrimestre());

    Asignatura creada = asignaturaService.crear(asignatura);
    return ResponseEntity.status(HttpStatus.CREATED).body(AsignaturaMapper.toResponse(creada));
  }

  @PutMapping("/{id}")
  public ResponseEntity<AsignaturaResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody AsignaturaRequestDTO request) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Asignatura existente =
        asignaturaService.obtenerPorIdDeUsuario(usuarioActual.getId(), id);

    Asignatura datos = new Asignatura();
    datos.setNombre(request.getNombre());
    datos.setDescripcion(request.getDescripcion());
    datos.setColorHex(request.getColorHex());
    datos.setTrimestre(request.getTrimestre());
    datos.setUsuario(existente.getUsuario());

    Asignatura actualizada = asignaturaService.actualizar(id, datos);
    return ResponseEntity.ok(AsignaturaMapper.toResponse(actualizada));
  }

  @GetMapping("/{id}/resumen-trimestres")
  public ResponseEntity<Map<String, Long>> resumenTrimestres(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    return ResponseEntity.ok(asignaturaService.resumenTrimestres(usuarioActual.getId(), id));
  }

  @GetMapping("/{id}/temas-planos")
  public ResponseEntity<List<TemaPlanoResponseDTO>> temasPlanos(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    return ResponseEntity.ok(asignaturaService.temasPlanos(usuarioActual.getId(), id));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    asignaturaService.obtenerPorIdDeUsuario(usuarioActual.getId(), id);

    asignaturaService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

}
