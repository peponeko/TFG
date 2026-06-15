package com.easy4you.mapper;

import com.easy4you.dto.resumen.ResumenResponseDTO;
import com.easy4you.model.entity.Resumen;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;

public final class ResumenMapper {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private ResumenMapper() {}

  public static ResumenResponseDTO toResponse(Resumen resumen) {
    List<String> puntosClave = parseStringList(resumen.getPuntosClaveJson());

    return new ResumenResponseDTO(
        resumen.getId(),
        resumen.getUsuario() != null ? resumen.getUsuario().getId() : null,
        resumen.getTema() != null ? resumen.getTema().getId() : null,
        resumen.getDocumento() != null ? resumen.getDocumento().getId() : null,
        resumen.getTitulo(),
        resumen.getContenido(),
        puntosClave,
        resumen.getOrigen(),
        resumen.getCreatedAt(),
        resumen.getUpdatedAt());
  }

  private static List<String> parseStringList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
      if (list == null) {
        return List.of();
      }
      return list.stream().filter(Objects::nonNull).toList();
    } catch (Exception ex) {
      return List.of();
    }
  }
}

