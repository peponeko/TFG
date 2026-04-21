package com.easy4you.service;

import com.easy4you.model.entity.Asignatura;
import java.util.List;

public interface AsignaturaService {
  List<Asignatura> listar();

  List<Asignatura> listarPorUsuarioId(Long usuarioId);

  Asignatura crear(Asignatura asignatura);

  Asignatura obtenerPorId(Long id);

  Asignatura actualizar(Long id, Asignatura asignatura);

  void eliminar(Long id);
}
