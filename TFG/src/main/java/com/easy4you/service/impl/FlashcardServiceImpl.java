package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Flashcard;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.service.FlashcardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlashcardServiceImpl implements FlashcardService {

  private final FlashcardRepository flashcardRepository;

  @Override
  public Flashcard crear(Flashcard flashcard) {
    return flashcardRepository.save(flashcard);
  }

  @Override
  @Transactional(readOnly = true)
  public Flashcard obtenerPorId(Long id) {
    return flashcardRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Flashcard no encontrada: " + id));
  }

  @Override
  public Flashcard actualizar(Long id, Flashcard datos) {
    Flashcard existente = obtenerPorId(id);
    existente.setPregunta(datos.getPregunta());
    existente.setRespuesta(datos.getRespuesta());
    if (datos.getDificultad() != null) {
      existente.setDificultad(datos.getDificultad());
    }
    if (datos.getDocumento() != null) {
      existente.setDocumento(datos.getDocumento());
    }
    return flashcardRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!flashcardRepository.existsById(id)) {
      throw new NotFoundException("Flashcard no encontrada: " + id);
    }
    flashcardRepository.deleteById(id);
  }
}
