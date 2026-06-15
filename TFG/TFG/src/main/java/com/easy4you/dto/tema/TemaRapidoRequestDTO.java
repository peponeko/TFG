package com.easy4you.dto.tema;

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
public class TemaRapidoRequestDTO {

  @NotNull private Long asignaturaId;

  // 1,2,3 o 0/null (General)
  private Integer trimestre;

  private Long unidadTematicaId;

  @NotBlank
  @Size(max = 200)
  private String titulo;

  private String descripcion;

  @Size(max = 500)
  private String palabrasClave;
}

