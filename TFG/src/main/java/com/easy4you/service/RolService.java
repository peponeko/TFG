package com.easy4you.service;

import com.easy4you.model.entity.Rol;

public interface RolService {
  Rol crear(Rol rol);

  Rol obtenerPorId(Long id);

  Rol actualizar(Long id, Rol rol);

  void eliminar(Long id);
}

