package com.easy4you.dto.unidad;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnidadResponseDTO {
  private Long id;
  private Long resultadoAprendizajeId;
  private String titulo;
  private String descripcion;
  private Integer orden;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

