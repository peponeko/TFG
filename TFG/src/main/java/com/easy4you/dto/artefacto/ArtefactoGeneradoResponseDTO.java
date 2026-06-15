package com.easy4you.dto.artefacto;

import com.easy4you.model.enums.EstadoArtefactoGenerado;
import com.easy4you.model.enums.TipoArtefactoGenerado;
import java.time.LocalDateTime;

public record ArtefactoGeneradoResponseDTO(
    Long id,
    Long asignaturaId,
    TipoArtefactoGenerado tipo,
    EstadoArtefactoGenerado estado,
    String rutaArchivo,
    String metadatosJson,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}