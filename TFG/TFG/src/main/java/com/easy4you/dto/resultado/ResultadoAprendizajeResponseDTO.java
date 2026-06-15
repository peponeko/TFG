package com.easy4you.dto.resultado;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoAprendizajeResponseDTO {
  private Long id;
  private Long asignaturaId;
  private String codigo;
  private String descripcion;
  private Integer orden;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

