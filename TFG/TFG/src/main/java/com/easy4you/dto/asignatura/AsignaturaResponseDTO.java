package com.easy4you.dto.asignatura;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AsignaturaResponseDTO {
  private Long id;
  private Long usuarioId;
  private String nombre;
  private String descripcion;
  private String colorHex;
  private Integer trimestre;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
