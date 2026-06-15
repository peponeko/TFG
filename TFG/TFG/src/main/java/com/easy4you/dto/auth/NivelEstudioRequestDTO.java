package com.easy4you.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NivelEstudioRequestDTO {

  @NotBlank
  @Pattern(
      regexp = "universitario|ciclo-superior|ciclo-medio|eso-bachillerato|primaria|no-estudiante")
  private String nivelEstudio;
}
