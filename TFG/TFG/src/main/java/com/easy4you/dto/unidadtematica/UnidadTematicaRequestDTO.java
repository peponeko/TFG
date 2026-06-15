package com.easy4you.dto.unidadtematica;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnidadTematicaRequestDTO {

  @NotNull
  private Long asignaturaId;

  @NotBlank
  @Size(max = 200)
  private String titulo;

  private Integer orden;

  private Integer trimestre;
}

