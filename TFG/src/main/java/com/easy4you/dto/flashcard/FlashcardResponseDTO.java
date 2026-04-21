package com.easy4you.dto.flashcard;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardResponseDTO {
  private Long id;
  private Long usuarioId;
  private Long temaId;
  private Long documentoId;
  private Long chunkOrigenId;
  private String pregunta;
  private String respuesta;
  private Integer dificultad;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

