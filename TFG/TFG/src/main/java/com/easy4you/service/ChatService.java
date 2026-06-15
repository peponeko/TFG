package com.easy4you.service;

import com.easy4you.model.entity.ChatConversacion;
import com.easy4you.model.entity.ChatMensaje;
import java.util.List;

public interface ChatService {

  ChatConversacion crearConversacion(Long usuarioId, Long asignaturaId, Long temaId, String titulo);

  List<ChatConversacion> listarConversaciones(Long usuarioId);

  ChatConversacion obtenerConversacion(Long usuarioId, Long conversacionId);

  void eliminarConversacion(Long usuarioId, Long conversacionId);

  List<ChatMensaje> listarMensajes(Long usuarioId, Long conversacionId);

  ChatSendMessageResult enviarMensaje(Long usuarioId, Long conversacionId, String contenido);

  ChatConversacion actualizarFuentes(Long usuarioId, Long conversacionId, List<Long> documentoIds);
}

