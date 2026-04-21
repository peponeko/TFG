package com.easy4you.service;

import com.easy4you.model.entity.Tema;
import java.util.List;

public interface TemaService {
  List<Tema> listar();

  List<Tema> listarPorUnidadId(Long unidadId);

  Tema crear(Tema tema);

  Tema obtenerPorId(Long id);

  Tema actualizar(Long id, Tema tema);

  void eliminar(Long id);
}
