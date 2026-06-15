package com.easy4you.dto.chat;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversacionCreateRequestDTO {
  private Long asignaturaId;
  private Long temaId;

  @Size(max = 200)
  private String titulo;
}

