package com.easy4you.dto.progreso;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgresoAsignaturaResponseDTO {
  private Long asignaturaId;
  private BigDecimal porcentaje;
}

