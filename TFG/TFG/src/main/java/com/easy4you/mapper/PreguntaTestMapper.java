package com.easy4you.mapper;

import com.easy4you.dto.pregunta.PreguntaTestOptionDTO;
import com.easy4you.dto.pregunta.PreguntaTestResponseDTO;
import com.easy4you.model.entity.PreguntaTest;
import com.easy4you.model.entity.PreguntaTestOpcion;
import java.util.List;
import java.util.Objects;

public final class PreguntaTestMapper {
  private PreguntaTestMapper() {}

  public static PreguntaTestResponseDTO toResponse(PreguntaTest p, List<PreguntaTestOpcion> opciones) {
    List<PreguntaTestOptionDTO> opcionesDto =
        opciones == null
            ? List.of()
            : opciones.stream()
                .filter(Objects::nonNull)
                .map(o -> new PreguntaTestOptionDTO(o.getId(), o.getTexto(), o.getOrden()))
                .toList();

    return new PreguntaTestResponseDTO(
        p.getId(),
        p.getUsuario() != null ? p.getUsuario().getId() : null,
        p.getTema() != null ? p.getTema().getId() : null,
        p.getDocumento() != null ? p.getDocumento().getId() : null,
        p.getChunkOrigen() != null ? p.getChunkOrigen().getId() : null,
        p.getEnunciado(),
        p.getExplicacion(),
        p.getDificultad(),
        opcionesDto,
        p.getCreatedAt(),
        p.getUpdatedAt());
  }
}

