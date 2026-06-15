package com.easy4you.service;

import com.easy4you.model.entity.ChatMensaje;
import java.util.List;

public record ChatSendMessageResult(ChatMensaje userMessage, ChatMensaje assistantMessage, List<ChatSource> sources) {}

