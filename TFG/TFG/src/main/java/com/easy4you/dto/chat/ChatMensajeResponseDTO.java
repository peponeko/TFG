package com.easy4you.dto.chat;

import com.easy4you.model.enums.ChatRol;
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
public class ChatMensajeResponseDTO {
  private Long id;
  private Long conversacionId;
  private ChatRol rol;
  private String contenido;
  private List<ChatFuenteDTO> fuentes;
  private LocalDateTime createdAt;
}

