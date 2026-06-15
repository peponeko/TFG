package com.easy4you.service;

import com.easy4you.model.entity.SesionEstudio;

public interface SesionEstudioService {
  SesionEstudio crear(SesionEstudio sesionEstudio);

  SesionEstudio obtenerPorId(Long id);

  SesionEstudio actualizar(Long id, SesionEstudio sesionEstudio);

  void eliminar(Long id);
}

