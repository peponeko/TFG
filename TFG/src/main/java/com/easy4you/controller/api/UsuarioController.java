package com.easy4you.controller.api;

import com.easy4you.dto.usuario.UsuarioRequestDTO;
import com.easy4you.dto.usuario.UsuarioResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.model.entity.Rol;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.RolRepository;
import com.easy4you.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

  private final UsuarioService usuarioService;
  private final RolRepository rolRepository;
  private final PasswordEncoder passwordEncoder;

  @GetMapping
  public ResponseEntity<List<UsuarioResponseDTO>> listar() {
    List<UsuarioResponseDTO> response =
        usuarioService.listar().stream().map(this::toResponse).toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UsuarioResponseDTO> obtener(@PathVariable Long id) {
    return ResponseEntity.ok(toResponse(usuarioService.obtenerPorId(id)));
  }

  @PostMapping
  public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioRequestDTO request) {
    if (request.getPassword() == null || request.getPassword().isBlank()) {
      throw new BadRequestException("La contraseña es obligatoria");
    }

    Usuario usuario = new Usuario();
    usuario.setNombre(request.getNombre());
    usuario.setApellidos(request.getApellidos());
    usuario.setEmail(request.getEmail());
    usuario.setImagenUrl(request.getImagenUrl());
    usuario.setActivo(request.getActivo() != null ? request.getActivo() : true);
    usuario.setVerificado(request.getVerificado() != null ? request.getVerificado() : false);
    usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));

    if (request.getRoles() != null) {
      usuario.setRoles(resolveRoles(request.getRoles()));
    }

    Usuario creado = usuarioService.crear(usuario);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(creado));
  }

  @PutMapping("/{id}")
  public ResponseEntity<UsuarioResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody UsuarioRequestDTO request) {

    Usuario existente = usuarioService.obtenerPorId(id);

    Usuario datos = new Usuario();
    datos.setNombre(request.getNombre());
    datos.setApellidos(request.getApellidos());
    datos.setEmail(request.getEmail());
    datos.setImagenUrl(request.getImagenUrl());
    datos.setActivo(request.getActivo() != null ? request.getActivo() : existente.isActivo());
    datos.setVerificado(
        request.getVerificado() != null ? request.getVerificado() : existente.isVerificado());
    datos.setUltimoLogin(existente.getUltimoLogin());

    if (request.getPassword() != null && !request.getPassword().isBlank()) {
      datos.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    }
    if (request.getRoles() != null) {
      datos.setRoles(resolveRoles(request.getRoles()));
    }

    Usuario actualizado = usuarioService.actualizar(id, datos);
    return ResponseEntity.ok(toResponse(actualizado));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    usuarioService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  private UsuarioResponseDTO toResponse(Usuario usuario) {
    Set<String> roles =
        usuario.getRoles() == null
            ? Set.of()
            : usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet());

    return new UsuarioResponseDTO(
        usuario.getId(),
        usuario.getNombre(),
        usuario.getApellidos(),
        usuario.getEmail(),
        usuario.getImagenUrl(),
        usuario.isActivo(),
        usuario.isVerificado(),
        usuario.getUltimoLogin(),
        roles,
        usuario.getCreatedAt(),
        usuario.getUpdatedAt());
  }

  private Set<Rol> resolveRoles(Set<String> nombresRol) {
    return nombresRol.stream()
        .map(
            nombre ->
                rolRepository
                    .findTopByNombreIgnoreCaseOrderByIdAsc(nombre)
                    .orElseThrow(() -> new BadRequestException("Rol no válido: " + nombre)))
        .collect(Collectors.toSet());
  }
}
