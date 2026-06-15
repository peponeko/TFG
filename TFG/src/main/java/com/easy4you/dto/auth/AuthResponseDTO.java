package com.easy4you.dto.auth;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
  private String token;
  private String tokenType;
  private Long usuarioId;
  private String email;
  private Set<String> roles;
}

