package com.easy4you.service.impl;

import com.easy4you.dto.evento.EventoCalendarioRequestDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.EventoCalendario;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.EventoCalendarioRepository;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.service.EventoCalendarioService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventoCalendarioServiceImpl implements EventoCalendarioService {

  private final EventoCalendarioRepository eventoCalendarioRepository;
  private final AsignaturaRepository asignaturaRepository;
  private final UsuarioRepository usuarioRepository;

  @Override
  @Transactional(readOnly = true)
  public List<EventoCalendario> listar(Long usuarioId, LocalDate desde, LocalDate hasta) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }

    if (desde == null && hasta == null) {
      return eventoCalendarioRepository.findByUsuarioIdOrderByFechaInicioAsc(usuarioId);
    }

    if (desde == null || hasta == null) {
      throw new BadRequestException("desde y hasta deben venir ambos, o ninguno");
    }
    if (hasta.isBefore(desde)) {
      throw new BadRequestException("hasta no puede ser anterior a desde");
    }

    return eventoCalendarioRepository.findByUsuarioIdAndFechaInicioBetweenOrderByFechaInicioAsc(
        usuarioId, desde, hasta);
  }

  @Override
  @Transactional(readOnly = true)
  public EventoCalendario obtenerPorIdDeUsuario(Long usuarioId, Long id) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (id == null) {
      throw new BadRequestException("id es obligatorio");
    }
    return eventoCalendarioRepository
        .findByIdAndUsuarioId(id, usuarioId)
        .orElseThrow(() -> new NotFoundException("Evento no encontrado: " + id));
  }

  @Override
  public EventoCalendario crearDeUsuario(Long usuarioId, EventoCalendarioRequestDTO request) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (request == null) {
      throw new BadRequestException("Body es obligatorio");
    }

    EventoCalendario evento = new EventoCalendario();
    evento.setUsuario(resolveUsuario(usuarioId));
    evento.setTitulo(request.getTitulo() != null ? request.getTitulo().trim() : null);
    evento.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
    evento.setFechaInicio(request.getFechaInicio());
    evento.setHoraInicio(request.getHoraInicio());
    evento.setTipo(request.getTipo());
    evento.setAsignatura(resolveAsignatura(usuarioId, request.getAsignaturaId()));
    evento.setCompletado(false);
    return eventoCalendarioRepository.save(evento);
  }

  @Override
  public EventoCalendario actualizarDeUsuario(Long usuarioId, Long id, EventoCalendarioRequestDTO request) {
    if (request == null) {
      throw new BadRequestException("Body es obligatorio");
    }

    EventoCalendario existente = obtenerPorIdDeUsuario(usuarioId, id);
    existente.setTitulo(request.getTitulo() != null ? request.getTitulo().trim() : null);
    existente.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
    existente.setFechaInicio(request.getFechaInicio());
    existente.setHoraInicio(request.getHoraInicio());
    existente.setTipo(request.getTipo());
    existente.setAsignatura(resolveAsignatura(usuarioId, request.getAsignaturaId()));
    return eventoCalendarioRepository.save(existente);
  }

  @Override
  public void eliminarDeUsuario(Long usuarioId, Long id) {
    EventoCalendario existente = obtenerPorIdDeUsuario(usuarioId, id);
    eventoCalendarioRepository.delete(existente);
  }

  @Override
  public EventoCalendario toggleCompletado(Long usuarioId, Long id) {
    EventoCalendario existente = obtenerPorIdDeUsuario(usuarioId, id);
    existente.setCompletado(!existente.isCompletado());
    return eventoCalendarioRepository.save(existente);
  }

  private Asignatura resolveAsignatura(Long usuarioId, Long asignaturaId) {
    if (asignaturaId == null) {
      return null;
    }
    return asignaturaRepository
        .findByIdAndUsuarioId(asignaturaId, usuarioId)
        .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + asignaturaId));
  }

  private Usuario resolveUsuario(Long usuarioId) {
    return usuarioRepository
        .findById(usuarioId)
        .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + usuarioId));
  }
}

