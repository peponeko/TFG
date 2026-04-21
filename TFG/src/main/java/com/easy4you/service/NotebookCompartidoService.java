package com.easy4you.service;

import com.easy4you.model.entity.NotebookCompartido;
import com.easy4you.model.enums.RolNotebookCompartido;
import java.util.List;
import java.util.Optional;

public interface NotebookCompartidoService {
  NotebookCompartido compartir(Long propietarioId, Long asignaturaId, Long usuarioInvitadoId, RolNotebookCompartido rol);

  List<NotebookCompartido> listarCompartidosConmigo(Long usuarioInvitadoId);

  void revocar(Long propietarioId, Long asignaturaId, Long usuarioInvitadoId);

  /**
   * Verifica si un usuario tiene acceso a un notebook (ya sea como propietario o invitado)
   */
  boolean tieneAcceso(Long usuarioId, Long asignaturaId);

  /**
   * Obtiene el rol de un usuario en un notebook compartido
   * @return El rol del usuario, o empty si no tiene acceso
   */
  Optional<RolNotebookCompartido> obtenerRol(Long usuarioId, Long asignaturaId);

  /**
   * Verifica si un usuario puede editar un notebook (rol EDITOR o es el propietario)
   */
  boolean puedeEditar(Long usuarioId, Long asignaturaId);

  /**
   * Obtiene el ID del propietario de un notebook
   */
  Long obtenerPropietarioId(Long asignaturaId);
}

