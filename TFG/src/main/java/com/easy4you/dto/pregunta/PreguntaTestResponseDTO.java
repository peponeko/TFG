package com.easy4you.dto.pregunta;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaTestResponseDTO {
  private Long id;
  private Long usuarioId;
  private Long temaId;
  private Long documentoId;
  private Long chunkOrigenId;
  private String enunciado;
  private String explicacion;
  private Integer dificultad;
  private List<PreguntaTestOptionDTO> opciones;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

