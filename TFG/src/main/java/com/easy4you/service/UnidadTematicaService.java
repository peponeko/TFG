package com.easy4you.service;

import com.easy4you.dto.unidadtematica.UnidadTematicaRequestDTO;
import com.easy4you.model.entity.UnidadTematica;
import java.util.List;

public interface UnidadTematicaService {

  List<UnidadTematica> listarPorAsignaturaDeUsuario(Long usuarioId, Long asignaturaId);

  UnidadTematica obtenerPorIdDeUsuario(Long usuarioId, Long id);

  UnidadTematica crearDeUsuario(Long usuarioId, UnidadTematicaRequestDTO request);

  UnidadTematica actualizarDeUsuario(Long usuarioId, Long id, UnidadTematicaRequestDTO request);

  void eliminarDeUsuario(Long usuarioId, Long id);
}

