package com.easy4you.service.impl;

import com.easy4you.dto.auth.AuthResponseDTO;
import com.easy4you.exception.UnauthorizedException;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.security.JwtUtil;
import com.easy4you.service.AuthRefreshService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthRefreshServiceImpl implements AuthRefreshService {

  private static final Duration REFRESH_GRACE = Duration.ofMinutes(30);

  @Value("${app.jwt.secret}")
  private String jwtSecret;

  private final JwtUtil jwtUtil;
  private final UsuarioRepository usuarioRepository;

  @Override
  @Transactional(readOnly = true)
  public AuthResponseDTO refresh(String token) {
    String t = (token == null) ? null : token.trim();
    if (t == null || t.isBlank()) {
      throw new UnauthorizedException("Token inválido");
    }

    Claims claims = parseClaimsAllowExpired(t);
    String email = claims.getSubject();
    if (email == null || email.isBlank()) {
      throw new UnauthorizedException("Token inválido");
    }

    Date exp = claims.getExpiration();
    if (exp == null) {
      throw new UnauthorizedException("Token inválido");
    }

    Instant now = Instant.now();
    Instant expiration = exp.toInstant();
    if (expiration.isBefore(now.minus(REFRESH_GRACE))) {
      // Demasiado caducado: forzar login
      throw new UnauthorizedException("Token caducado");
    }

    Set<String> roles = extractRoles(claims);

    Usuario usuario =
        usuarioRepository
            .findTopByEmailOrderByIdAsc(email)
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

    String newToken = jwtUtil.generateTokenWithRoles(email, roles);
    return new AuthResponseDTO(
        newToken, "Bearer", usuario.getId(), usuario.getEmail(), usuario.getNivelEstudio(), roles);
  }

  private Claims parseClaimsAllowExpired(String token) {
    try {
      return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    } catch (ExpiredJwtException ex) {
      // Expirado pero firma válida: devolvemos claims para decidir la ventana de refresco
      return ex.getClaims();
    } catch (SecurityException
        | MalformedJwtException
        | UnsupportedJwtException
        | IllegalArgumentException ex) {
      throw new UnauthorizedException("Token inválido");
    }
  }

  private Set<String> extractRoles(Claims claims) {
    Object raw = claims.get("roles");
    if (raw == null) {
      return Set.of();
    }
    if (raw instanceof List<?> list) {
      Set<String> out = new LinkedHashSet<>();
      for (Object o : list) {
        if (o == null) continue;
        String s = String.valueOf(o).trim();
        if (!s.isBlank()) out.add(s);
      }
      return out;
    }
    // fallback
    String s = String.valueOf(raw).trim();
    return s.isBlank() ? Set.of() : Set.of(s);
  }

  private SecretKey getSigningKey() {
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new IllegalStateException("JWT secret no configurado (app.jwt.secret / JWT_SECRET)");
    }
    byte[] keyBytes;
    try {
      keyBytes = Decoders.BASE64.decode(jwtSecret);
    } catch (IllegalArgumentException ex) {
      keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    }
    if (keyBytes.length < 32) {
      throw new IllegalStateException("JWT secret demasiado corto. Usa al menos 32 bytes (256 bits).");
    }
    return Keys.hmacShaKeyFor(keyBytes);
  }
}

