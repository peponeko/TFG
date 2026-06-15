package com.easy4you.mapper;

import com.easy4you.dto.asignatura.AsignaturaResponseDTO;
import com.easy4you.model.entity.Asignatura;

public final class AsignaturaMapper {
  private AsignaturaMapper() {}

  public static AsignaturaResponseDTO toResponse(Asignatura asignatura) {
    return new AsignaturaResponseDTO(
        asignatura.getId(),
        asignatura.getUsuario() != null ? asignatura.getUsuario().getId() : null,
        asignatura.getNombre(),
        asignatura.getDescripcion(),
        asignatura.getColorHex(),
        asignatura.getTrimestre(),
        asignatura.getCreatedAt(),
        asignatura.getUpdatedAt());
  }
}

