package com.easy4you.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  @Value("${app.jwt.secret}")
  private String jwtSecret;

  @Value("${app.jwt.expiration-minutes:60}")
  private long expirationMinutes;

  public String generateToken(String username, Collection<? extends GrantedAuthority> authorities) {
    Set<String> roles =
        authorities == null
            ? Set.of()
            : authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    return generateTokenWithRoles(username, roles);
  }

  public String generateTokenWithRoles(String username, Collection<String> roles) {
    Map<String, Object> claims = Map.of("roles", roles == null ? List.of() : List.copyOf(roles));
    return buildToken(claims, username);
  }

  public String generateToken(UserDetails userDetails) {
    return generateToken(userDetails.getUsername(), userDetails.getAuthorities());
  }

  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    try {
      String username = extractUsername(token);
      return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    } catch (Exception ex) {
      return false;
    }
  }

  private String buildToken(Map<String, Object> extraClaims, String subject) {
    Instant now = Instant.now();
    Instant expiration = now.plus(Duration.ofMinutes(expirationMinutes));

    return Jwts.builder()
        .claims(extraClaims)
        .subject(subject)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiration))
        .signWith(getSigningKey())
        .compact();
  }

  private boolean isTokenExpired(String token) {
    Date expiration = extractAllClaims(token).getExpiration();
    return expiration.before(new Date());
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes;
    try {
      keyBytes = Decoders.BASE64.decode(jwtSecret);
    } catch (IllegalArgumentException ex) {
      keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    }
    return Keys.hmacShaKeyFor(keyBytes);
  }
}

