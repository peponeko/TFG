package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.ActividadRepaso;
import com.easy4you.repository.ActividadRepasoRepository;
import com.easy4you.service.ActividadRepasoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ActividadRepasoServiceImpl implements ActividadRepasoService {

  private final ActividadRepasoRepository actividadRepasoRepository;

  @Override
  public ActividadRepaso crear(ActividadRepaso actividadRepaso) {
    return actividadRepasoRepository.save(actividadRepaso);
  }

  @Override
  @Transactional(readOnly = true)
  public ActividadRepaso obtenerPorId(Long id) {
    return actividadRepasoRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Actividad de repaso no encontrada: " + id));
  }

  @Override
  public ActividadRepaso actualizar(Long id, ActividadRepaso datos) {
    ActividadRepaso existente = obtenerPorId(id);
    existente.setTipo(datos.getTipo());
    existente.setEnunciado(datos.getEnunciado());
    existente.setSolucion(datos.getSolucion());
    if (datos.getDocumento() != null) {
      existente.setDocumento(datos.getDocumento());
    }
    return actividadRepasoRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!actividadRepasoRepository.existsById(id)) {
      throw new NotFoundException("Actividad de repaso no encontrada: " + id);
    }
    actividadRepasoRepository.deleteById(id);
  }
}

