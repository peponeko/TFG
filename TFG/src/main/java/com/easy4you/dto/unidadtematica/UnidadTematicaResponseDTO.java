package com.easy4you.dto.unidadtematica;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnidadTematicaResponseDTO {
  private Long id;
  private Long asignaturaId;
  private String titulo;
  private Integer orden;
  private Integer trimestre;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

