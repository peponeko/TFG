package com.easy4you.service.impl;

import com.easy4you.dto.tema.TemaRapidoRequestDTO;
import com.easy4you.dto.tema.TemaRequestDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Tema;
import com.easy4you.model.entity.UnidadTematica;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.repository.UnidadTematicaRepository;
import com.easy4you.service.TemaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TemaServiceImpl implements TemaService {

  private final TemaRepository temaRepository;
  private final AsignaturaRepository asignaturaRepository;
  private final UnidadTematicaRepository unidadTematicaRepository;

  @Override
  @Transactional(readOnly = true)
  public List<Tema> listar() {
    return temaRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Tema> listarPorUnidadId(Long unidadId) {
    throw new BadRequestException("unidadId ya no se usa en el esquema simplificado. Usa asignaturaId.");
  }

  @Override
  public Tema crear(Tema tema) {
    return temaRepository.save(tema);
  }

  @Override
  @Transactional(readOnly = true)
  public Tema obtenerPorId(Long id) {
    return temaRepository.findById(id).orElseThrow(() -> new NotFoundException("Tema no encontrado: " + id));
  }

  @Override
  public Tema actualizar(Long id, Tema datos) {
    Tema existente = obtenerPorId(id);
    if (datos.getAsignatura() != null) {
      existente.setAsignatura(datos.getAsignatura());
    }
    existente.setTrimestre(datos.getTrimestre());
    existente.setTitulo(datos.getTitulo());
    existente.setDescripcion(datos.getDescripcion());
    existente.setOrden(datos.getOrden());
    existente.setPalabrasClave(datos.getPalabrasClave());
    return temaRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!temaRepository.existsById(id)) {
      throw new NotFoundException("Tema no encontrado: " + id);
    }
    temaRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Tema> listarPorAsignaturaIdDeUsuario(Long usuarioId, Long asignaturaId) {
    if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioId).isEmpty()) {
      throw new NotFoundException("Asignatura no encontrada: " + asignaturaId);
    }
    return temaRepository.findByAsignaturaIdOrderByOrdenAsc(asignaturaId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Tema> listarPorUnidadIdDeUsuario(Long usuarioId, Long unidadId) {
    throw new BadRequestException("unidadId ya no se usa en el esquema simplificado. Usa asignaturaId.");
  }

  @Override
  @Transactional(readOnly = true)
  public Tema obtenerPorIdDeUsuario(Long usuarioId, Long temaId) {
    return temaRepository
        .findByIdAndAsignaturaUsuarioId(temaId, usuarioId)
        .orElseThrow(() -> new NotFoundException("Tema no encontrado: " + temaId));
  }

  @Override
  public Tema crearDeUsuario(Long usuarioId, TemaRequestDTO request) {
    Tema tema = new Tema();
    Asignatura asignatura = resolveAsignaturaParaTema(usuarioId, request);
    tema.setAsignatura(asignatura);
    tema.setUnidadTematica(resolveUnidadTematica(usuarioId, asignatura.getId(), request.getUnidadTematicaId()));
    tema.setTitulo(request.getTitulo());
    tema.setDescripcion(request.getDescripcion());
    tema.setOrden(request.getOrden() != null ? request.getOrden() : 0);
    tema.setPalabrasClave(request.getPalabrasClave());
    tema.setTrimestre(null);

    return crear(tema);
  }

  @Override
  public Tema crearRapidoDeUsuario(Long usuarioId, TemaRapidoRequestDTO request) {
    if (request.getAsignaturaId() == null) {
      throw new BadRequestException("asignaturaId es obligatorio");
    }

    Integer trimestre = request.getTrimestre();
    if (trimestre != null && !(trimestre == 0 || trimestre == 1 || trimestre == 2 || trimestre == 3)) {
      throw new BadRequestException("trimestre inválido. Usa 1, 2, 3 o 0/null");
    }

    Asignatura asignatura =
        asignaturaRepository
            .findByIdAndUsuarioId(request.getAsignaturaId(), usuarioId)
            .orElseThrow(
                () -> new NotFoundException("Asignatura no encontrada: " + request.getAsignaturaId()));

    Tema tema = new Tema();
    tema.setAsignatura(asignatura);
    tema.setUnidadTematica(resolveUnidadTematica(usuarioId, asignatura.getId(), request.getUnidadTematicaId()));
    tema.setTrimestre(trimestre == null || trimestre == 0 ? null : trimestre);
    tema.setTitulo(request.getTitulo());
    tema.setDescripcion(request.getDescripcion());
    tema.setOrden(0);
    tema.setPalabrasClave(request.getPalabrasClave());

    return crear(tema);
  }

  @Override
  public Tema actualizarDeUsuario(Long usuarioId, Long temaId, TemaRequestDTO request) {
    Tema existente = obtenerPorIdDeUsuario(usuarioId, temaId);

    Tema datos = new Tema();
    datos.setAsignatura(existente.getAsignatura());
    datos.setUnidadTematica(
        resolveUnidadTematica(usuarioId, existente.getAsignatura().getId(), request.getUnidadTematicaId()));
    datos.setTrimestre(existente.getTrimestre());
    datos.setTitulo(request.getTitulo());
    datos.setDescripcion(request.getDescripcion());
    datos.setOrden(request.getOrden() != null ? request.getOrden() : existente.getOrden());
    datos.setPalabrasClave(request.getPalabrasClave());

    return actualizar(temaId, datos);
  }

  @Override
  public void eliminarDeUsuario(Long usuarioId, Long temaId) {
    obtenerPorIdDeUsuario(usuarioId, temaId);
    eliminar(temaId);
  }

  private Asignatura resolveAsignaturaParaTema(Long usuarioId, TemaRequestDTO request) {
    if (request == null) {
      throw new BadRequestException("Body es obligatorio");
    }

    if (request.getAsignaturaId() == null) {
      throw new BadRequestException("asignaturaId es obligatorio");
    }

    return asignaturaRepository
        .findByIdAndUsuarioId(request.getAsignaturaId(), usuarioId)
        .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + request.getAsignaturaId()));
  }

  private UnidadTematica resolveUnidadTematica(Long usuarioId, Long asignaturaId, Long unidadTematicaId) {
    if (unidadTematicaId == null) {
      return null;
    }
    UnidadTematica unidad =
        unidadTematicaRepository
            .findByIdAndAsignaturaUsuarioId(unidadTematicaId, usuarioId)
            .orElseThrow(() -> new NotFoundException("Unidad temática no encontrada: " + unidadTematicaId));
    if (unidad.getAsignatura() == null || unidad.getAsignatura().getId() == null) {
      throw new BadRequestException("Unidad temática inválida");
    }
    if (!unidad.getAsignatura().getId().equals(asignaturaId)) {
      throw new BadRequestException("La unidad temática no pertenece a la asignatura del tema");
    }
    return unidad;
  }
}
