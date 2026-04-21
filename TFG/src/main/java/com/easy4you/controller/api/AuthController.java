package com.easy4you.controller.api;

import com.easy4you.dto.auth.AuthResponseDTO;
import com.easy4you.dto.auth.LoginRequestDTO;
import com.easy4you.dto.auth.RegisterRequestDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.UnauthorizedException;
import com.easy4you.model.entity.Rol;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.RolRepository;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.security.JwtUtil;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final UsuarioRepository usuarioRepository;
  private final RolRepository rolRepository;

  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
    try {
      Authentication auth =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

      String token = jwtUtil.generateToken(auth.getName(), auth.getAuthorities());
      Set<String> roles =
          auth.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority)
              .collect(Collectors.toSet());

      Usuario usuario =
          usuarioRepository
              .findByEmail(request.getEmail())
              .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

      return ResponseEntity.ok(new AuthResponseDTO(token, "Bearer", usuario.getId(), usuario.getEmail(), roles));
    } catch (BadCredentialsException ex) {
      throw new UnauthorizedException("Credenciales inválidas");
    }
  }

  @PostMapping("/register")
  @Transactional
  public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
    if (usuarioRepository.existsByEmail(request.getEmail())) {
      throw new BadRequestException("Ya existe un usuario con ese email");
    }

    Rol rolPorDefecto = resolveRolPorDefecto();

    Usuario usuario = new Usuario();
    usuario.setNombre(request.getNombre());
    usuario.setApellidos(request.getApellidos());
    usuario.setEmail(request.getEmail());
    usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    usuario.setActivo(true);
    usuario.setVerificado(false);
    usuario.setRoles(Set.of(rolPorDefecto));

    Usuario creado = usuarioRepository.save(usuario);

    Set<String> roles =
        creado.getRoles().stream().map(r -> "ROLE_" + normalizeRoleName(r.getNombre())).collect(Collectors.toSet());
    String token = jwtUtil.generateTokenWithRoles(creado.getEmail(), roles);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new AuthResponseDTO(token, "Bearer", creado.getId(), creado.getEmail(), roles));
  }

  private Rol resolveRolPorDefecto() {
    return rolRepository
        .findTopByNombreIgnoreCaseOrderByIdAsc("USER")
        .or(() -> rolRepository.findTopByNombreIgnoreCaseOrderByIdAsc("ESTUDIANTE"))
        .orElseGet(
            () -> {
              Rol rol = new Rol();
              rol.setNombre("USER");
              rol.setDescripcion("Usuario estándar");
              return rolRepository.save(rol);
            });
  }

  private String normalizeRoleName(String nombre) {
    if (nombre == null) {
      return "USER";
    }
    if ("ESTUDIANTE".equalsIgnoreCase(nombre)) {
      return "USER";
    }
    return nombre.toUpperCase();
  }
}
