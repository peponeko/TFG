package com.easy4you.mapper;

import com.easy4you.dto.flashcard.FlashcardResponseDTO;
import com.easy4you.model.entity.Flashcard;

public final class FlashcardMapper {
  private FlashcardMapper() {}

  public static FlashcardResponseDTO toResponse(Flashcard fc) {
    return new FlashcardResponseDTO(
        fc.getId(),
        fc.getUsuario() != null ? fc.getUsuario().getId() : null,
        fc.getTema() != null ? fc.getTema().getId() : null,
        fc.getDocumento() != null ? fc.getDocumento().getId() : null,
        fc.getChunkOrigen() != null ? fc.getChunkOrigen().getId() : null,
        fc.getPregunta(),
        fc.getRespuesta(),
        fc.getDificultad(),
        fc.getCreatedAt(),
        fc.getUpdatedAt());
  }
}

