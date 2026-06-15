package com.easy4you.controller.api;

import com.easy4you.dto.chat.ChatConversacionCreateRequestDTO;
import com.easy4you.dto.chat.ChatConversacionResponseDTO;
import com.easy4you.dto.chat.ChatFuenteDTO;
import com.easy4you.dto.chat.ChatMensajeCreateRequestDTO;
import com.easy4you.dto.chat.ChatMensajeResponseDTO;
import com.easy4you.dto.chat.ChatSendMessageResponseDTO;
import com.easy4you.dto.chat.ChatUpdateFuentesRequestDTO;
import com.easy4you.model.entity.ChatConversacion;
import com.easy4you.model.entity.ChatMensaje;
import com.easy4you.model.entity.Usuario;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.ChatSendMessageResult;
import com.easy4you.service.ChatService;
import com.easy4you.service.ChatSource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;
  private final AuthenticatedUserService authenticatedUserService;
  private final ObjectMapper objectMapper;

  @PostMapping("/conversaciones")
  public ResponseEntity<ChatConversacionResponseDTO> crearConversacion(
      @Valid @RequestBody ChatConversacionCreateRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    ChatConversacion created =
        chatService.crearConversacion(
            usuarioActual.getId(), request.getAsignaturaId(), request.getTemaId(), request.getTitulo());

    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
  }

  @GetMapping("/conversaciones")
  public ResponseEntity<List<ChatConversacionResponseDTO>> listarConversaciones() {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    List<ChatConversacionResponseDTO> response =
        chatService.listarConversaciones(usuarioActual.getId()).stream().map(this::toResponse).toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/conversaciones/{id}")
  public ResponseEntity<ChatConversacionResponseDTO> obtenerConversacion(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    ChatConversacion conversacion = chatService.obtenerConversacion(usuarioActual.getId(), id);
    return ResponseEntity.ok(toResponse(conversacion));
  }

  @DeleteMapping("/conversaciones/{id}")
  public ResponseEntity<Void> eliminarConversacion(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    chatService.eliminarConversacion(usuarioActual.getId(), id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/conversaciones/{id}/mensajes")
  public ResponseEntity<List<ChatMensajeResponseDTO>> listarMensajes(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    List<ChatMensaje> mensajes = chatService.listarMensajes(usuarioActual.getId(), id);
    List<ChatMensajeResponseDTO> response = mensajes.stream().map(this::toMensajeResponseWithoutSources).toList();
    return ResponseEntity.ok(response);
  }

  @PostMapping("/conversaciones/{id}/mensajes")
  public ResponseEntity<ChatSendMessageResponseDTO> enviarMensaje(
      @PathVariable Long id, @Valid @RequestBody ChatMensajeCreateRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    ChatSendMessageResult result = chatService.enviarMensaje(usuarioActual.getId(), id, request.getContenido());

    ChatMensajeResponseDTO userMessage = toMensajeResponseWithoutSources(result.userMessage());
    ChatMensajeResponseDTO assistantMessage = toMensajeResponseWithSources(result.assistantMessage(), result.sources());

    return ResponseEntity.ok(new ChatSendMessageResponseDTO(userMessage, assistantMessage));
  }

  @PutMapping("/conversaciones/{id}/fuentes")
  public ResponseEntity<ChatConversacionResponseDTO> actualizarFuentes(
      @PathVariable Long id, @RequestBody ChatUpdateFuentesRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    ChatConversacion updated =
        chatService.actualizarFuentes(usuarioActual.getId(), id, request != null ? request.getDocumentoIds() : null);
    return ResponseEntity.ok(toResponse(updated));
  }

  private ChatConversacionResponseDTO toResponse(ChatConversacion conversacion) {
    List<Long> fuentesActivas = parseLongList(conversacion.getFuentesActivasJson());
    return new ChatConversacionResponseDTO(
        conversacion.getId(),
        conversacion.getUsuario() != null ? conversacion.getUsuario().getId() : null,
        conversacion.getAsignatura() != null ? conversacion.getAsignatura().getId() : null,
        conversacion.getTema() != null ? conversacion.getTema().getId() : null,
        conversacion.getTitulo(),
        fuentesActivas,
        conversacion.getCreatedAt(),
        conversacion.getUpdatedAt());
  }

  private ChatMensajeResponseDTO toMensajeResponseWithoutSources(ChatMensaje mensaje) {
    return new ChatMensajeResponseDTO(
        mensaje.getId(),
        mensaje.getConversacion() != null ? mensaje.getConversacion().getId() : null,
        mensaje.getRol(),
        mensaje.getContenido(),
        List.of(),
        mensaje.getCreatedAt());
  }

  private ChatMensajeResponseDTO toMensajeResponseWithSources(ChatMensaje mensaje, List<ChatSource> sources) {
    List<ChatFuenteDTO> fuentes =
        sources == null
            ? List.of()
            : sources.stream()
                .filter(Objects::nonNull)
                .map(
                    s ->
                        new ChatFuenteDTO(
                            s.chunkId(), s.documentoId(), s.documentoNombre(), s.indiceChunk(), s.paginaOrigen()))
                .toList();

    return new ChatMensajeResponseDTO(
        mensaje.getId(),
        mensaje.getConversacion() != null ? mensaje.getConversacion().getId() : null,
        mensaje.getRol(),
        mensaje.getContenido(),
        fuentes,
        mensaje.getCreatedAt());
  }

  private List<Long> parseLongList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<Long> list = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
      if (list == null) {
        return List.of();
      }
      return list.stream().filter(Objects::nonNull).distinct().toList();
    } catch (Exception ex) {
      return List.of();
    }
  }
}

