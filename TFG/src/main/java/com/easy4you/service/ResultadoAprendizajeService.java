package com.easy4you.service;

import com.easy4you.model.entity.ResultadoAprendizaje;

public interface ResultadoAprendizajeService {
  ResultadoAprendizaje crear(ResultadoAprendizaje resultadoAprendizaje);

  ResultadoAprendizaje obtenerPorId(Long id);

  ResultadoAprendizaje actualizar(Long id, ResultadoAprendizaje resultadoAprendizaje);

  void eliminar(Long id);
}

