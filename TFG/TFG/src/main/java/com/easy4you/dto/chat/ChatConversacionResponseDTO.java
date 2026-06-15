package com.easy4you.dto.chat;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversacionResponseDTO {
  private Long id;
  private Long usuarioId;
  private Long asignaturaId;
  private Long temaId;
  private String titulo;
  private List<Long> fuentesActivasDocumentoIds;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

