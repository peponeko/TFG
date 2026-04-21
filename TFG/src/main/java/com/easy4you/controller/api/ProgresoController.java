package com.easy4you.controller.api;

import com.easy4you.dto.progreso.ProgresoTemaResponseDTO;
import com.easy4you.dto.progreso.ProgresoUsuarioResponseDTO;
import com.easy4you.model.entity.Usuario;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.ProgresoUsuarioService;
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
}

