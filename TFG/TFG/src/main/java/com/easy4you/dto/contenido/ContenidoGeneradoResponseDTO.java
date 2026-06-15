package com.easy4you.dto.contenido;

import com.easy4you.model.enums.TipoContenidoGenerado;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContenidoGeneradoResponseDTO {
  private TipoContenidoGenerado tipo;
  private String resumen;
  private List<PreguntaTestGeneradaDTO> preguntas;
  private List<FlashcardGeneradaDTO> flashcards;
}

