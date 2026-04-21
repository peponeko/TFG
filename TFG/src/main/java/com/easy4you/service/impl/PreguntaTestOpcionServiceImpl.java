package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.PreguntaTestOpcion;
import com.easy4you.repository.PreguntaTestOpcionRepository;
import com.easy4you.service.PreguntaTestOpcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PreguntaTestOpcionServiceImpl implements PreguntaTestOpcionService {

  private final PreguntaTestOpcionRepository opcionRepository;

  @Override
  public PreguntaTestOpcion crear(PreguntaTestOpcion opcion) {
    return opcionRepository.save(opcion);
  }

  @Override
  @Transactional(readOnly = true)
  public PreguntaTestOpcion obtenerPorId(Long id) {
    return opcionRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Opción de pregunta test no encontrada: " + id));
  }

  @Override
  public PreguntaTestOpcion actualizar(Long id, PreguntaTestOpcion datos) {
    PreguntaTestOpcion existente = obtenerPorId(id);
    existente.setTexto(datos.getTexto());
    existente.setEsCorrecta(datos.isEsCorrecta());
    existente.setOrden(datos.getOrden());
    return opcionRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!opcionRepository.existsById(id)) {
      throw new NotFoundException("Opción de pregunta test no encontrada: " + id);
    }
    opcionRepository.deleteById(id);
  }
}
