package com.easy4you.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.easy4you.dto.auth.AuthResponseDTO;
import com.easy4you.dto.auth.LoginRequestDTO;
import com.easy4you.dto.auth.NivelEstudioRequestDTO;
import com.easy4you.dto.auth.RegisterRequestDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.UnauthorizedException;
import com.easy4you.model.entity.Rol;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.RolRepository;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.security.JwtUtil;
import com.easy4you.service.impl.AuthServiceImpl;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock AuthenticationManager authenticationManager;
  @Mock PasswordEncoder passwordEncoder;
  @Mock JwtUtil jwtUtil;
  @Mock UsuarioRepository usuarioRepository;
  @Mock RolRepository rolRepository;

  @InjectMocks AuthServiceImpl authService;

  @Test
  void login_devuelveTokenYDatosBasicos() {
    LoginRequestDTO req = new LoginRequestDTO("u@x.com", "pass");

    Authentication auth =
        new UsernamePasswordAuthenticationToken("u@x.com", "pass", Set.of(() -> "ROLE_USER"));
    when(authenticationManager.authenticate(any())).thenReturn(auth);
    when(jwtUtil.generateToken(any(), any())).thenReturn("token123");

    Usuario u = new Usuario();
    u.setId(10L);
    u.setEmail("u@x.com");
    u.setNivelEstudio("universitario");
    when(usuarioRepository.findTopByEmailOrderByIdAsc("u@x.com")).thenReturn(Optional.of(u));

    AuthResponseDTO out = authService.login(req);

    assertThat(out.getToken()).isEqualTo("token123");
    assertThat(out.getUsuarioId()).isEqualTo(10L);
    assertThat(out.getEmail()).isEqualTo("u@x.com");
    assertThat(out.getNivelEstudio()).isEqualTo("universitario");
    assertThat(out.getRoles()).contains("ROLE_USER");
  }

  @Test
  void login_lanzaUnauthorizedSiCredencialesMalas() {
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
    assertThrows(UnauthorizedException.class, () -> authService.login(new LoginRequestDTO("x", "y")));
  }

  @Test
  void register_lanzaBadRequestSiEmailExiste() {
    when(usuarioRepository.existsByEmail("a@b.com")).thenReturn(true);
    RegisterRequestDTO req = new RegisterRequestDTO("A", "B", "a@b.com", "Pass1234!");
    assertThrows(BadRequestException.class, () -> authService.register(req));
  }

  @Test
  void register_creaUsuarioYDevuelveToken() {
    RegisterRequestDTO req = new RegisterRequestDTO("A", "B", "a@b.com", "Pass1234!");

    when(usuarioRepository.existsByEmail("a@b.com")).thenReturn(false);

    Rol rol = new Rol();
    rol.setNombre("USER");
    when(rolRepository.findTopByNombreIgnoreCaseOrderByIdAsc("USER")).thenReturn(Optional.of(rol));

    when(passwordEncoder.encode("Pass1234!")).thenReturn("hash");
    when(jwtUtil.generateTokenWithRoles(any(), any())).thenReturn("token");

    when(usuarioRepository.save(any())).thenAnswer(inv -> {
      Usuario u = inv.getArgument(0);
      u.setId(1L);
      return u;
    });

    AuthResponseDTO out = authService.register(req);

    assertThat(out.getToken()).isEqualTo("token");
    assertThat(out.getUsuarioId()).isEqualTo(1L);
    assertThat(out.getEmail()).isEqualTo("a@b.com");
    assertThat(out.getNivelEstudio()).isNull();
    assertThat(out.getRoles()).contains("ROLE_USER");
  }

  @Test
  void actualizarNivelEstudio_persisteElValorNormalizado() {
    Usuario u = new Usuario();
    u.setId(22L);
    u.setEmail("u@x.com");

    when(usuarioRepository.findById(22L)).thenReturn(Optional.of(u));
    when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Usuario updated =
        authService.actualizarNivelEstudio(22L, new NivelEstudioRequestDTO("  UNIVERSITARIO  "));

    assertThat(updated.getNivelEstudio()).isEqualTo("universitario");
  }
}

