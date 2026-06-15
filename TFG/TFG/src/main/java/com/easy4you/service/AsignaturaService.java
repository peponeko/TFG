package com.easy4you.service;

import com.easy4you.dto.tema.TemaPlanoResponseDTO;
import com.easy4you.model.entity.Asignatura;
import java.util.List;
import java.util.Map;

public interface AsignaturaService {
  List<Asignatura> listar();

  List<Asignatura> listarPorUsuarioId(Long usuarioId);

  List<Asignatura> listarPorUsuarioIdYTrimestre(Long usuarioId, Integer trimestre);

  Asignatura obtenerPorIdDeUsuario(Long usuarioId, Long asignaturaId);

  Asignatura crear(Asignatura asignatura);

  Asignatura obtenerPorId(Long id);

  Asignatura actualizar(Long id, Asignatura asignatura);

  void eliminar(Long id);

  Map<String, Long> resumenTrimestres(Long usuarioId, Long asignaturaId);

  List<TemaPlanoResponseDTO> temasPlanos(Long usuarioId, Long asignaturaId);
}
