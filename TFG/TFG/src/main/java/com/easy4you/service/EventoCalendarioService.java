package com.easy4you.service;

import com.easy4you.dto.evento.EventoCalendarioRequestDTO;
import com.easy4you.model.entity.EventoCalendario;
import java.time.LocalDate;
import java.util.List;

public interface EventoCalendarioService {

  List<EventoCalendario> listar(Long usuarioId, LocalDate desde, LocalDate hasta);

  EventoCalendario obtenerPorIdDeUsuario(Long usuarioId, Long id);

  EventoCalendario crearDeUsuario(Long usuarioId, EventoCalendarioRequestDTO request);

  EventoCalendario actualizarDeUsuario(Long usuarioId, Long id, EventoCalendarioRequestDTO request);

  void eliminarDeUsuario(Long usuarioId, Long id);

  EventoCalendario toggleCompletado(Long usuarioId, Long id);
}

