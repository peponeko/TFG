package com.easy4you.service;

import com.easy4you.model.entity.Nota;
import java.util.List;

public interface NotaService {
  Nota crear(Nota nota);

  Nota obtenerPorId(Long usuarioId, Long id);

  List<Nota> listar(Long usuarioId, Long documentoId, Long temaId, Long asignaturaId);

  Nota actualizar(Long usuarioId, Long id, Nota nota);

  void eliminar(Long usuarioId, Long id);
}

