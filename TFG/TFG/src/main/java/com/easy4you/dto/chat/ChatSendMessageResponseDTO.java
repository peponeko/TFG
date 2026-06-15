package com.easy4you.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSendMessageResponseDTO {
  private ChatMensajeResponseDTO userMessage;
  private ChatMensajeResponseDTO assistantMessage;
}

