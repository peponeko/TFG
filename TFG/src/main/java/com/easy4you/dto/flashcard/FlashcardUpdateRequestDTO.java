package com.easy4you.dto.flashcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardUpdateRequestDTO {
  @NotBlank
  @Size(max = 4000)
  private String pregunta;

  @NotBlank
  @Size(max = 8000)
  private String respuesta;

  private Integer dificultad;
}

