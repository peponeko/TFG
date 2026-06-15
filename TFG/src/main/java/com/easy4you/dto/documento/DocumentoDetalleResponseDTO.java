package com.easy4you.dto.documento;

import com.easy4you.dto.flashcard.FlashcardResponseDTO;
import com.easy4you.dto.pregunta.PreguntaTestResponseDTO;
import com.easy4you.dto.resumen.ResumenResponseDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoDetalleResponseDTO {
  private DocumentoResponseDTO documento;
  private DocumentoChunksPageResponseDTO chunks;
  private List<ResumenResponseDTO> resumenes;
  private List<FlashcardResponseDTO> flashcards;
  private List<PreguntaTestResponseDTO> preguntas;
}

