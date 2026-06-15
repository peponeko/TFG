package com.easy4you.service;

import com.easy4you.model.entity.ProgresoTema;

public interface ProgresoTemaService {
  ProgresoTema crear(ProgresoTema progresoTema);

  ProgresoTema obtenerPorId(Long id);

  ProgresoTema actualizar(Long id, ProgresoTema progresoTema);

  void eliminar(Long id);
}

