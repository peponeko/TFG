package com.easy4you.controller.api;

import com.easy4you.dto.auth.AuthResponseDTO;
import com.easy4you.dto.auth.LoginRequestDTO;
import com.easy4you.dto.auth.NivelEstudioRequestDTO;
import com.easy4you.dto.auth.PerfilUpdateRequestDTO;
import com.easy4you.dto.auth.RegisterRequestDTO;
import com.easy4you.model.entity.Usuario;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.AuthService;
import com.easy4you.service.AuthRefreshService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final AuthenticatedUserService authenticatedUserService;
  private final AuthRefreshService authRefreshService;

  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponseDTO> refresh(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
    String token = extractBearerToken(authHeader);
    return ResponseEntity.ok(authRefreshService.refresh(token));
  }

  @PutMapping("/perfil")
  public ResponseEntity<Void> actualizarPerfil(@Valid @RequestBody PerfilUpdateRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    authService.actualizarPerfil(usuarioActual.getId(), request);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/nivel-estudio")
  public ResponseEntity<Void> actualizarNivelEstudio(@Valid @RequestBody NivelEstudioRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    authService.actualizarNivelEstudio(usuarioActual.getId(), request);
    return ResponseEntity.noContent().build();
  }

  private String extractBearerToken(String header) {
    if (header == null) return null;
    String h = header.trim();
    if (h.length() < 8) return null;
    if (h.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return h.substring(7).trim();
    }
    return null;
  }
}
