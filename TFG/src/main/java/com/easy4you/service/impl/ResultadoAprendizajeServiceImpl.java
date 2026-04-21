package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.ResultadoAprendizaje;
import com.easy4you.repository.ResultadoAprendizajeRepository;
import com.easy4you.service.ResultadoAprendizajeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResultadoAprendizajeServiceImpl implements ResultadoAprendizajeService {

  private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;

  @Override
  public ResultadoAprendizaje crear(ResultadoAprendizaje resultadoAprendizaje) {
    return resultadoAprendizajeRepository.save(resultadoAprendizaje);
  }

  @Override
  @Transactional(readOnly = true)
  public ResultadoAprendizaje obtenerPorId(Long id) {
    return resultadoAprendizajeRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Resultado de aprendizaje no encontrado: " + id));
  }

  @Override
  public ResultadoAprendizaje actualizar(Long id, ResultadoAprendizaje datos) {
    ResultadoAprendizaje existente = obtenerPorId(id);
    existente.setCodigo(datos.getCodigo());
    existente.setDescripcion(datos.getDescripcion());
    existente.setOrden(datos.getOrden());
    return resultadoAprendizajeRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!resultadoAprendizajeRepository.existsById(id)) {
      throw new NotFoundException("Resultado de aprendizaje no encontrado: " + id);
    }
    resultadoAprendizajeRepository.deleteById(id);
  }
}

