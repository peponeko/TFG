package com.easy4you.mapper;

import com.easy4you.dto.documento.DocumentoResponseDTO;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.enums.EstadoProcesadoDocumento;

public final class DocumentoMapper {
  private DocumentoMapper() {}

  public static DocumentoResponseDTO toResponse(Documento documento) {
    EstadoProcesadoDocumento estadoProcesado = documento.getEstadoProcesado();
    if (estadoProcesado == EstadoProcesadoDocumento.LISTO) {
      estadoProcesado = EstadoProcesadoDocumento.PROCESADO;
    }

    return new DocumentoResponseDTO(
        documento.getId(),
        documento.getUsuario() != null ? documento.getUsuario().getId() : null,
        documento.getAsignatura() != null ? documento.getAsignatura().getId() : null,
        documento.getTema() != null ? documento.getTema().getId() : null,
        documento.getNombreOriginal(),
        documento.getRutaArchivo(),
        documento.getMimeType(),
        documento.getExtension(),
        documento.getTamanoBytes(),
        documento.getChecksumSha256(),
        documento.getPaginas(),
        estadoProcesado,
        documento.getErrorExtraccion(),
        documento.getCreatedAt(),
        documento.getUpdatedAt());
  }
}

