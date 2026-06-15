package com.easy4you.service;

import com.easy4you.dto.flashcard.FlashcardResponseDTO;
import com.easy4you.model.entity.Flashcard;
import java.util.List;

public interface FlashcardService {
  Flashcard crear(Flashcard flashcard);

  Flashcard obtenerPorId(Long id);

  Flashcard actualizar(Long id, Flashcard flashcard);

  void eliminar(Long id);

  List<FlashcardResponseDTO> listarPorDocumento(Long usuarioId, Long documentoId);

  List<FlashcardResponseDTO> listarPorTema(Long usuarioId, Long temaId);
}
