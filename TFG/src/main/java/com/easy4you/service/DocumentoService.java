package com.easy4you.service;

import com.easy4you.model.entity.Documento;
import java.util.List;

public interface DocumentoService {
  List<Documento> listar();

  List<Documento> listarPorUsuarioId(Long usuarioId);

  List<Documento> listarPorTemaId(Long temaId);

  Documento crear(Documento documento);

  Documento obtenerPorId(Long id);

  Documento actualizar(Long id, Documento documento);

  void eliminar(Long id);
}
