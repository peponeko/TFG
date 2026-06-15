package com.easy4you.dto.usuario;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {
  private Long id;
  private String nombre;
  private String apellidos;
  private String email;
  private String nivelEstudio;
  private String imagenUrl;
  private boolean activo;
  private boolean verificado;
  private LocalDateTime ultimoLogin;
  private Set<String> roles;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
