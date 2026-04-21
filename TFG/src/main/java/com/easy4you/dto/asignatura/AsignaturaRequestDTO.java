package com.easy4you.dto.asignatura;

import jakarta.validation.constraints.NotNull;
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
public class AsignaturaRequestDTO {
  @NotNull
  private Long usuarioId;

  @NotBlank
  @Size(max = 120)
  private String nombre;
  private String descripcion;

  @Size(max = 7)
  private String colorHex;
}
