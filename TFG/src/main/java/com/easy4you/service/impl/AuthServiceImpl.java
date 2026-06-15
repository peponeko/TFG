package com.easy4you.service.impl;

import com.easy4you.dto.auth.AuthResponseDTO;
import com.easy4you.dto.auth.LoginRequestDTO;
import com.easy4you.dto.auth.NivelEstudioRequestDTO;
import com.easy4you.dto.auth.PerfilUpdateRequestDTO;
import com.easy4you.dto.auth.RegisterRequestDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.UnauthorizedException;
import com.easy4you.model.entity.Rol;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.RolRepository;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.security.JwtUtil;
import com.easy4you.service.AuthService;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private static final Set<String> NIVELES_ESTUDIO_VALIDOS =
      Set.of(
          "universitario",
          "ciclo-superior",
          "ciclo-medio",
          "eso-bachillerato",
          "primaria",
          "no-estudiante");

  private final AuthenticationManager authenticationManager;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final UsuarioRepository usuarioRepository;
  private final RolRepository rolRepository;

  @Override
  public AuthResponseDTO login(@Valid LoginRequestDTO request) {
    try {
      Authentication auth =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

      String token = jwtUtil.generateToken(auth.getName(), auth.getAuthorities());
      Set<String> roles =
          auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

      Usuario usuario =
          usuarioRepository
              .findTopByEmailOrderByIdAsc(request.getEmail())
              .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

      return new AuthResponseDTO(
          token, "Bearer", usuario.getId(), usuario.getEmail(), usuario.getNivelEstudio(), roles);
    } catch (BadCredentialsException ex) {
      throw new UnauthorizedException("Credenciales inválidas");
    }
  }

  @Override
  @Transactional
  public AuthResponseDTO register(@Valid RegisterRequestDTO request) {
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
    usuario.setNivelEstudio(null);
    usuario.setRoles(Set.of(rolPorDefecto));

    Usuario creado = usuarioRepository.save(usuario);

    Set<String> roles =
        creado.getRoles().stream()
            .map(r -> "ROLE_" + normalizeRoleName(r.getNombre()))
            .collect(Collectors.toSet());
    String token = jwtUtil.generateTokenWithRoles(creado.getEmail(), roles);

    return new AuthResponseDTO(
        token, "Bearer", creado.getId(), creado.getEmail(), creado.getNivelEstudio(), roles);
  }

  @Override
  @Transactional
  public Usuario actualizarPerfil(Long usuarioId, PerfilUpdateRequestDTO request) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (request == null) {
      throw new BadRequestException("Body es obligatorio");
    }

    Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

    usuario.setNombre(request.getNombre() != null ? request.getNombre().trim() : null);
    usuario.setApellidos(request.getApellidos() != null ? request.getApellidos().trim() : null);

    return usuarioRepository.save(usuario);
  }

  @Override
  @Transactional
  public Usuario actualizarNivelEstudio(Long usuarioId, NivelEstudioRequestDTO request) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (request == null) {
      throw new BadRequestException("Body es obligatorio");
    }

    String nivel = normalizeNivelEstudio(request.getNivelEstudio());
    if (!NIVELES_ESTUDIO_VALIDOS.contains(nivel)) {
      throw new BadRequestException("Nivel de estudio no válido");
    }

    Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

    usuario.setNivelEstudio(nivel);
    return usuarioRepository.save(usuario);
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

  private String normalizeNivelEstudio(String valor) {
    if (valor == null) {
      return null;
    }
    return valor.trim().toLowerCase();
  }
}

