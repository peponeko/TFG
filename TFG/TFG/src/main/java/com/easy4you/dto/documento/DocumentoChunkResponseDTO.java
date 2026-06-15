package com.easy4you.dto.documento;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoChunkResponseDTO {
  private Long id;
  private Long documentoId;
  private Integer indiceChunk;
  private String texto;
  private Integer paginaOrigen;
  private Integer tokenCount;
}

