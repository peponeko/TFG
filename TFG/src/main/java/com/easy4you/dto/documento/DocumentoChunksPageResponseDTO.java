package com.easy4you.dto.documento;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoChunksPageResponseDTO {
  private List<DocumentoChunkResponseDTO> items;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
}

