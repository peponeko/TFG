package com.easy4you.service;

import com.easy4you.model.entity.ActividadRepaso;

public interface ActividadRepasoService {
  ActividadRepaso crear(ActividadRepaso actividadRepaso);

  ActividadRepaso obtenerPorId(Long id);

  ActividadRepaso actualizar(Long id, ActividadRepaso actividadRepaso);

  void eliminar(Long id);
}

