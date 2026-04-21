package com.easy4you.dto.documento;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoBusquedaFragmentDTO {
  private Long chunkId;
  private Integer indiceChunk;
  private Integer paginaOrigen;
  private String fragmento;
}

