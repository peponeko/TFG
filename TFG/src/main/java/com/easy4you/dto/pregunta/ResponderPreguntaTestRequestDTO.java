package com.easy4you.dto.pregunta;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResponderPreguntaTestRequestDTO {
  @NotNull
  @Min(0)
  @Max(3)
  private Integer indiceOpcion;
}

