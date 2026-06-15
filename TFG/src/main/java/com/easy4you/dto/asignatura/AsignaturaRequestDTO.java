package com.easy4you.dto.asignatura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
  private Long usuarioId;

  @NotBlank
  @Size(max = 120)
  private String nombre;
  private String descripcion;

  @Size(max = 7)
  private String colorHex;

  // nullable, valores 1, 2 o 3
  @Min(1)
  @Max(3)
  private Integer trimestre;
}
