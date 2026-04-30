package com.easy4you.service;

import com.easy4you.model.entity.Resumen;
import java.util.List;

public interface ResumenService {
  Resumen crear(Resumen resumen);

  Resumen obtenerPorId(Long id);

  Resumen actualizar(Long id, Resumen resumen);

  void eliminar(Long id);

  List<Resumen> listarPorDocumento(Long usuarioId, Long documentoId);

  List<Resumen> listarPorTema(Long usuarioId, Long temaId);
}
