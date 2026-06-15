package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.SesionEstudio;
import com.easy4you.repository.SesionEstudioRepository;
import com.easy4you.service.SesionEstudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SesionEstudioServiceImpl implements SesionEstudioService {

  private final SesionEstudioRepository sesionEstudioRepository;

  @Override
  public SesionEstudio crear(SesionEstudio sesionEstudio) {
    return sesionEstudioRepository.save(sesionEstudio);
  }

  @Override
  @Transactional(readOnly = true)
  public SesionEstudio obtenerPorId(Long id) {
    return sesionEstudioRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Sesión de estudio no encontrada: " + id));
  }

  @Override
  public SesionEstudio actualizar(Long id, SesionEstudio datos) {
    SesionEstudio existente = obtenerPorId(id);
    existente.setTitulo(datos.getTitulo());
    existente.setDescripcion(datos.getDescripcion());
    existente.setFechaInicio(datos.getFechaInicio());
    existente.setFechaFin(datos.getFechaFin());
    existente.setEstado(datos.getEstado());
    existente.setMinutosObjetivo(datos.getMinutosObjetivo());
    existente.setMinutosReal(datos.getMinutosReal());

    if (datos.getTemas() != null) {
      existente.getTemas().clear();
      existente.getTemas().addAll(datos.getTemas());
    }

    return sesionEstudioRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!sesionEstudioRepository.existsById(id)) {
      throw new NotFoundException("Sesión de estudio no encontrada: " + id);
    }
    sesionEstudioRepository.deleteById(id);
  }
}

