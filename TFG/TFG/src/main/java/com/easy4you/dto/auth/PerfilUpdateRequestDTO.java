package com.easy4you.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerfilUpdateRequestDTO {

  @NotBlank
  @Size(max = 100)
  private String nombre;

  @Size(max = 150)
  private String apellidos;
}

