package com.easy4you.dto.tema;

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
public class TemaRequestDTO {
  private Long unidadId;
  private Long asignaturaId;

  @NotBlank
  @Size(max = 200)
  private String titulo;
  private String descripcion;
  private Integer orden;

  @Size(max = 500)
  private String palabrasClave;
}
