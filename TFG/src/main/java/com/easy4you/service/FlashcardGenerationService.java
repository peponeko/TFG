package com.easy4you.service;

import com.easy4you.model.entity.Flashcard;
import java.util.List;

public interface FlashcardGenerationService {
  List<Flashcard> generarParaDocumento(Long usuarioId, Long documentoId);

  void solicitarGeneracionParaDocumento(Long usuarioId, Long documentoId);

  void generarParaDocumentoAsync(Long usuarioId, Long documentoId);
}
