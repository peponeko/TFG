package com.easy4you.service;

import com.easy4you.model.entity.PreguntaTestOpcion;

public interface PreguntaTestOpcionService {
  PreguntaTestOpcion crear(PreguntaTestOpcion opcion);

  PreguntaTestOpcion obtenerPorId(Long id);

  PreguntaTestOpcion actualizar(Long id, PreguntaTestOpcion opcion);

  void eliminar(Long id);
}

