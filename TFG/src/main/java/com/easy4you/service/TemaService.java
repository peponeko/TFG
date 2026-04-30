package com.easy4you.service;

import com.easy4you.dto.tema.TemaRapidoRequestDTO;
import com.easy4you.dto.tema.TemaRequestDTO;
import com.easy4you.model.entity.Tema;
import java.util.List;

public interface TemaService {
  List<Tema> listar();

  List<Tema> listarPorUnidadId(Long unidadId);

  Tema crear(Tema tema);

  Tema obtenerPorId(Long id);

  Tema actualizar(Long id, Tema tema);

  void eliminar(Long id);

  List<Tema> listarPorAsignaturaIdDeUsuario(Long usuarioId, Long asignaturaId);

  List<Tema> listarPorUnidadIdDeUsuario(Long usuarioId, Long unidadId);

  Tema obtenerPorIdDeUsuario(Long usuarioId, Long temaId);

  Tema crearDeUsuario(Long usuarioId, TemaRequestDTO request);

  Tema crearRapidoDeUsuario(Long usuarioId, TemaRapidoRequestDTO request);

  Tema actualizarDeUsuario(Long usuarioId, Long temaId, TemaRequestDTO request);

  void eliminarDeUsuario(Long usuarioId, Long temaId);
}
