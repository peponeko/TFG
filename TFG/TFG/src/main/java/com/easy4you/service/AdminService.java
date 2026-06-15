package com.easy4you.service;

import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Usuario;
import java.util.List;
import java.util.Set;

public interface AdminService {
  long countUsuarios();

  long countAsignaturas();

  long countDocumentos();

  List<Usuario> listarUsuarios();

  Usuario obtenerUsuario(Long id);

  void eliminarUsuario(Long id);

  List<Asignatura> listarAsignaturas();

  Asignatura obtenerAsignatura(Long id);

  Usuario crearUsuario(Usuario usuario, String rawPassword, Set<String> roles);

  Asignatura crearAsignatura(Asignatura asignatura);
}

