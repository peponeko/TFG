package com.easy4you.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatFuenteDTO {
  private Long chunkId;
  private Long documentoId;
  private String documentoNombre;
  private Integer indiceChunk;
  private Integer paginaOrigen;
}

