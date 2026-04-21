package com.easy4you.dto.notebook;

import com.easy4you.dto.asignatura.AsignaturaResponseDTO;
import com.easy4you.dto.documento.DocumentoResponseDTO;
import com.easy4you.dto.flashcard.FlashcardResponseDTO;
import com.easy4you.dto.nota.NotaResponseDTO;
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
public class NotebookOverviewResponseDTO {
  private AsignaturaResponseDTO notebook;
  private List<DocumentoResponseDTO> documentos;
  private List<ResumenResponseDTO> resumenes;
  private List<FlashcardResponseDTO> flashcards;
  private List<PreguntaTestResponseDTO> preguntas;
  private List<NotaResponseDTO> notas;
}
