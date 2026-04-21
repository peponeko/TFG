package com.easy4you.dto.progreso;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgresoTemaResponseDTO {
  private Long temaId;
  private BigDecimal porcentaje;
  private Integer sesionesCompletadas;
  private Integer minutosEstudiados;
  private LocalDateTime ultimaSesion;
  private long flashcardsRepasadas;
  private long testsCompletados;
}

