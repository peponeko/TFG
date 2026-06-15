package com.easy4you.dto.documento;

import com.easy4you.model.enums.EstadoProcesadoDocumento;

public record DocumentoEstadoResponseDTO(
    Long id,
    EstadoProcesadoDocumento estadoProcesado,
    String errorExtraccion,
    long resumenesCount,
    long flashcardsCount,
    long preguntasCount) {}

