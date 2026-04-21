package com.easy4you.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequestDTO {
  @NotBlank
  @Size(max = 100)
  private String nombre;

  @Size(max = 150)
  private String apellidos;

  @NotBlank
  @Email
  @Size(max = 190)
  private String email;

  @Size(min = 6, max = 100)
  private String password; // se hashea antes de persistir

  @Size(max = 500)
  private String imagenUrl;
  private Boolean activo;
  private Boolean verificado;
  private Set<String> roles; // nombres de rol (p.ej. ADMIN, ESTUDIANTE)
}
