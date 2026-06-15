package com.easy4you.mapper;

import com.easy4you.dto.documento.DocumentoResponseDTO;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.enums.EstadoProcesadoDocumento;

public final class DocumentoMapper {
  private DocumentoMapper() {}

  private static final int TEXTO_PREVIEW_MAX = 8000;

  /**
   * Listados (GET /api/documentos): omite texto y error pesados para no cargar LONGTEXT ni inflar JSON.
   * El detalle (/api/documentos/{id}) sigue usando {@link #toResponse}.
   */
  public static DocumentoResponseDTO toListItem(Documento documento) {
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
        null,
        estadoProcesado,
        null,
        documento.getCreatedAt(),
        documento.getUpdatedAt());
  }

  public static DocumentoResponseDTO toResponse(Documento documento) {
    EstadoProcesadoDocumento estadoProcesado = documento.getEstadoProcesado();
    if (estadoProcesado == EstadoProcesadoDocumento.LISTO) {
      estadoProcesado = EstadoProcesadoDocumento.PROCESADO;
    }

    String texto = documento.getTextoExtraido();
    if (texto != null) {
      texto = texto.trim();
      if (texto.length() > TEXTO_PREVIEW_MAX) {
        texto = texto.substring(0, TEXTO_PREVIEW_MAX - 1).trim() + "…";
      }
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
        texto,
        estadoProcesado,
        documento.getErrorExtraccion(),
        documento.getCreatedAt(),
        documento.getUpdatedAt());
  }
}

