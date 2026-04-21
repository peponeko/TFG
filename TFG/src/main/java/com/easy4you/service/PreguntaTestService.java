package com.easy4you.service;

import com.easy4you.model.entity.PreguntaTest;

public interface PreguntaTestService {
  PreguntaTest crear(PreguntaTest preguntaTest);

  PreguntaTest obtenerPorId(Long id);

  PreguntaTest actualizar(Long id, PreguntaTest preguntaTest);

  void eliminar(Long id);
}

