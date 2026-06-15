package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.ProgresoAsignatura;
import com.easy4you.repository.ProgresoAsignaturaRepository;
import com.easy4you.service.ProgresoAsignaturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgresoAsignaturaServiceImpl implements ProgresoAsignaturaService {

  private final ProgresoAsignaturaRepository progresoAsignaturaRepository;

  @Override
  public ProgresoAsignatura crear(ProgresoAsignatura progresoAsignatura) {
    return progresoAsignaturaRepository.save(progresoAsignatura);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgresoAsignatura obtenerPorId(Long id) {
    return progresoAsignaturaRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Progreso por asignatura no encontrado: " + id));
  }

  @Override
  public ProgresoAsignatura actualizar(Long id, ProgresoAsignatura datos) {
    ProgresoAsignatura existente = obtenerPorId(id);
    existente.setPorcentaje(datos.getPorcentaje());
    existente.setSesionesCompletadas(datos.getSesionesCompletadas());
    existente.setMinutosEstudiados(datos.getMinutosEstudiados());
    existente.setUltimaSesion(datos.getUltimaSesion());
    return progresoAsignaturaRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!progresoAsignaturaRepository.existsById(id)) {
      throw new NotFoundException("Progreso por asignatura no encontrado: " + id);
    }
    progresoAsignaturaRepository.deleteById(id);
  }
}

