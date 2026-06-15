package com.easy4you.security;

import com.easy4you.exception.UnauthorizedException;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

  private final UsuarioRepository usuarioRepository;

  @Transactional(readOnly = true)
  public Usuario requireUsuarioActual() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new UnauthorizedException("No autenticado");
    }

    String email = authentication.getName();
    if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
      throw new UnauthorizedException("No autenticado");
    }

    return usuarioRepository
        .findTopByEmailOrderByIdAsc(email)
        .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado: " + email));
  }
}

