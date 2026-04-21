package com.easy4you.dto.contenido;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaTestGeneradaDTO {
  private String enunciado;
  private List<String> opciones;
  private Integer indiceCorrecto;
}

