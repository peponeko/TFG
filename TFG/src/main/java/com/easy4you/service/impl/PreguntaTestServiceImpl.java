package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.PreguntaTest;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.service.PreguntaTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PreguntaTestServiceImpl implements PreguntaTestService {

  private final PreguntaTestRepository preguntaTestRepository;

  @Override
  public PreguntaTest crear(PreguntaTest preguntaTest) {
    return preguntaTestRepository.save(preguntaTest);
  }

  @Override
  @Transactional(readOnly = true)
  public PreguntaTest obtenerPorId(Long id) {
    return preguntaTestRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Pregunta test no encontrada: " + id));
  }

  @Override
  public PreguntaTest actualizar(Long id, PreguntaTest datos) {
    PreguntaTest existente = obtenerPorId(id);
    existente.setEnunciado(datos.getEnunciado());
    existente.setExplicacion(datos.getExplicacion());
    existente.setDificultad(datos.getDificultad());
    if (datos.getDocumento() != null) {
      existente.setDocumento(datos.getDocumento());
    }
    return preguntaTestRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!preguntaTestRepository.existsById(id)) {
      throw new NotFoundException("Pregunta test no encontrada: " + id);
    }
    preguntaTestRepository.deleteById(id);
  }
}

