package com.easy4you.dto.pregunta;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResponderPreguntaTestResponseDTO {
  private boolean correcta;
  private Integer indiceCorrecto;
  private String explicacion;
}

