package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.service.UsuarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

  private final UsuarioRepository usuarioRepository;

  @Override
  @Transactional(readOnly = true)
  public List<Usuario> listar() {
    return usuarioRepository.findAll();
  }

  @Override
  public Usuario crear(Usuario usuario) {
    return usuarioRepository.save(usuario);
  }

  @Override
  @Transactional(readOnly = true)
  public Usuario obtenerPorId(Long id) {
    return usuarioRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + id));
  }

  @Override
  public Usuario actualizar(Long id, Usuario datos) {
    Usuario existente = obtenerPorId(id);
    existente.setNombre(datos.getNombre());
    existente.setApellidos(datos.getApellidos());
    existente.setEmail(datos.getEmail());
    existente.setImagenUrl(datos.getImagenUrl());
    existente.setActivo(datos.isActivo());
    existente.setVerificado(datos.isVerificado());
    existente.setUltimoLogin(datos.getUltimoLogin());

    if (datos.getPasswordHash() != null && !datos.getPasswordHash().isBlank()) {
      existente.setPasswordHash(datos.getPasswordHash());
    }
    if (datos.getRoles() != null) {
      existente.setRoles(datos.getRoles());
    }

    return usuarioRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!usuarioRepository.existsById(id)) {
      throw new NotFoundException("Usuario no encontrado: " + id);
    }
    usuarioRepository.deleteById(id);
  }
}
