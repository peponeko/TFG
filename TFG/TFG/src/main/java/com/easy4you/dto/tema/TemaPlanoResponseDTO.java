package com.easy4you.dto.tema;

public record TemaPlanoResponseDTO(
    Long id,
    String titulo,
    String descripcion,
    String palabrasClave,
    Long unidadTematicaId,
    Integer trimestre,
    long documentosCount,
    long flashcardsCount,
    long preguntasCount) {}

