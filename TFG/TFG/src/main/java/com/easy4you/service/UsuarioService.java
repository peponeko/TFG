package com.easy4you.service;

import com.easy4you.model.entity.Usuario;
import java.util.List;

public interface UsuarioService {
  List<Usuario> listar();

  Usuario crear(Usuario usuario);

  Usuario obtenerPorId(Long id);

  Usuario actualizar(Long id, Usuario usuario);

  void eliminar(Long id);
}
