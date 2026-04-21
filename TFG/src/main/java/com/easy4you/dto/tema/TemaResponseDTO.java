package com.easy4you.dto.tema;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemaResponseDTO {
  private Long id;
  private Long unidadId;
  private String titulo;
  private String descripcion;
  private Integer orden;
  private String palabrasClave;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
