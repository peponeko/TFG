package com.easy4you.dto.nota;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotaResponseDTO {
  private Long id;
  private Long usuarioId;
  private Long documentoId;
  private Long chunkId;
  private Long temaId;
  private String titulo;
  private String contenido;
  private String colorHex;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

