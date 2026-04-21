package com.easy4you.service;

import com.easy4you.model.entity.Flashcard;

public interface FlashcardService {
  Flashcard crear(Flashcard flashcard);

  Flashcard obtenerPorId(Long id);

  Flashcard actualizar(Long id, Flashcard flashcard);

  void eliminar(Long id);
}

