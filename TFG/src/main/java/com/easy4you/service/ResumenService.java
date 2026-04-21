package com.easy4you.service;

import com.easy4you.model.entity.Resumen;

public interface ResumenService {
  Resumen crear(Resumen resumen);

  Resumen obtenerPorId(Long id);

  Resumen actualizar(Long id, Resumen resumen);

  void eliminar(Long id);
}

