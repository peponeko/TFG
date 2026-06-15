package com.easy4you.service.impl;

import com.easy4you.dto.unidadtematica.UnidadTematicaRequestDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.UnidadTematica;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.UnidadTematicaRepository;
import com.easy4you.service.UnidadTematicaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UnidadTematicaServiceImpl implements UnidadTematicaService {

  private final UnidadTematicaRepository unidadTematicaRepository;
  private final AsignaturaRepository asignaturaRepository;

  @Override
  @Transactional(readOnly = true)
  public List<UnidadTematica> listarPorAsignaturaDeUsuario(Long usuarioId, Long asignaturaId) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (asignaturaId == null) {
      throw new BadRequestException("asignaturaId es obligatorio");
    }
    if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioId).isEmpty()) {
      throw new NotFoundException("Asignatura no encontrada: " + asignaturaId);
    }
    return unidadTematicaRepository.findByAsignaturaIdOrderByOrdenAsc(asignaturaId);
  }

  @Override
  @Transactional(readOnly = true)
  public UnidadTematica obtenerPorIdDeUsuario(Long usuarioId, Long id) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (id == null) {
      throw new BadRequestException("id es obligatorio");
    }
    return unidadTematicaRepository
        .findByIdAndAsignaturaUsuarioId(id, usuarioId)
        .orElseThrow(() -> new NotFoundException("Unidad temática no encontrada: " + id));
  }

  @Override
  public UnidadTematica crearDeUsuario(Long usuarioId, UnidadTematicaRequestDTO request) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (request == null) {
      throw new BadRequestException("Body es obligatorio");
    }
    if (request.getAsignaturaId() == null) {
      throw new BadRequestException("asignaturaId es obligatorio");
    }

    Asignatura asignatura =
        asignaturaRepository
            .findByIdAndUsuarioId(request.getAsignaturaId(), usuarioId)
            .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + request.getAsignaturaId()));

    UnidadTematica u = new UnidadTematica();
    u.setAsignatura(asignatura);
    u.setTitulo(request.getTitulo() != null ? request.getTitulo().trim() : null);
    u.setOrden(request.getOrden() != null ? request.getOrden() : 0);
    u.setTrimestre(request.getTrimestre());
    return unidadTematicaRepository.save(u);
  }

  @Override
  public UnidadTematica actualizarDeUsuario(Long usuarioId, Long id, UnidadTematicaRequestDTO request) {
    if (request == null) {
      throw new BadRequestException("Body es obligatorio");
    }

    UnidadTematica existente = obtenerPorIdDeUsuario(usuarioId, id);

    // La unidad sigue ligada a la misma asignatura; no permitimos moverla en esta iteración
    existente.setTitulo(request.getTitulo() != null ? request.getTitulo().trim() : null);
    existente.setOrden(request.getOrden() != null ? request.getOrden() : existente.getOrden());
    existente.setTrimestre(request.getTrimestre());

    return unidadTematicaRepository.save(existente);
  }

  @Override
  public void eliminarDeUsuario(Long usuarioId, Long id) {
    UnidadTematica existente = obtenerPorIdDeUsuario(usuarioId, id);
    unidadTematicaRepository.delete(existente);
  }
}

