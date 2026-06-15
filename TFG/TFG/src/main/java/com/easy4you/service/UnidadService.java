package com.easy4you.service;

import com.easy4you.model.entity.Unidad;

public interface UnidadService {
  Unidad crear(Unidad unidad);

  Unidad obtenerPorId(Long id);

  Unidad actualizar(Long id, Unidad unidad);

  void eliminar(Long id);
}

