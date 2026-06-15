package com.easy4you.service;

import com.easy4you.model.entity.ProgresoAsignatura;

public interface ProgresoAsignaturaService {
  ProgresoAsignatura crear(ProgresoAsignatura progresoAsignatura);

  ProgresoAsignatura obtenerPorId(Long id);

  ProgresoAsignatura actualizar(Long id, ProgresoAsignatura progresoAsignatura);

  void eliminar(Long id);
}

