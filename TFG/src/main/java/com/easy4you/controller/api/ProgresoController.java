package com.easy4you.controller.api;

import com.easy4you.dto.progreso.ProgresoTemaResponseDTO;
import com.easy4you.dto.progreso.ProgresoAsignaturaResponseDTO;
import com.easy4you.dto.progreso.ProgresoUsuarioResponseDTO;
import com.easy4you.repository.ProgresoAsignaturaRepository;
import com.easy4you.model.entity.Usuario;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.ProgresoUsuarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progreso")
@RequiredArgsConstructor
public class ProgresoController {

  private final AuthenticatedUserService authenticatedUserService;
  private final ProgresoUsuarioService progresoUsuarioService;
  private final ProgresoAsignaturaRepository progresoAsignaturaRepository;

  @GetMapping("/usuario")
  public ResponseEntity<ProgresoUsuarioResponseDTO> progresoUsuario() {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    return ResponseEntity.ok(progresoUsuarioService.obtenerProgresoUsuario(usuarioActual.getId()));
  }

  @GetMapping("/tema/{temaId}")
  public ResponseEntity<ProgresoTemaResponseDTO> progresoTema(@PathVariable Long temaId) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    return ResponseEntity.ok(progresoUsuarioService.obtenerProgresoTema(usuarioActual.getId(), temaId));
  }

  @GetMapping("/asignaturas")
  public ResponseEntity<List<ProgresoAsignaturaResponseDTO>> progresoAsignaturas() {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    List<ProgresoAsignaturaResponseDTO> response =
        progresoAsignaturaRepository.findByUsuarioId(usuarioActual.getId()).stream()
            .map(
                p ->
                    new ProgresoAsignaturaResponseDTO(
                        p.getAsignatura() != null ? p.getAsignatura().getId() : null, p.getPorcentaje()))
            .filter(p -> p.getAsignaturaId() != null)
            .toList();
    return ResponseEntity.ok(response);
  }
}

