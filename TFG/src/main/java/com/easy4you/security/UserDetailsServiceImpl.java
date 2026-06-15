package com.easy4you.security;

import com.easy4you.model.entity.Rol;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.UsuarioRepository;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UsuarioRepository usuarioRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Usuario usuario =
        usuarioRepository
            .findTopByEmailOrderByIdAsc(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

    return User.builder()
        .username(usuario.getEmail())
        .password(usuario.getPasswordHash())
        .disabled(!usuario.isActivo())
        .authorities(toAuthorities(usuario))
        .build();
  }

  private Collection<? extends GrantedAuthority> toAuthorities(Usuario usuario) {
    if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
      return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    return usuario.getRoles().stream()
        .filter(Objects::nonNull)
        .map(Rol::getNombre)
        .filter(Objects::nonNull)
        .map(this::normalizeRoleName)
        .distinct()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .toList();
  }

  private String normalizeRoleName(String nombre) {
    if ("ESTUDIANTE".equalsIgnoreCase(nombre)) {
      return "USER";
    }
    return nombre.toUpperCase();
  }
}
