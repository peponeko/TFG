package com.easy4you.service.impl;

import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Rol;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.RolRepository;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.service.AdminService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

  private final UsuarioRepository usuarioRepository;
  private final AsignaturaRepository asignaturaRepository;
  private final DocumentoRepository documentoRepository;
  private final RolRepository rolRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public long countUsuarios() {
    return usuarioRepository.count();
  }

  @Override
  public long countAsignaturas() {
    return asignaturaRepository.count();
  }

  @Override
  public long countDocumentos() {
    return documentoRepository.count();
  }

  @Override
  public List<Usuario> listarUsuarios() {
    return usuarioRepository.findAll();
  }

  @Override
  public Usuario obtenerUsuario(Long id) {
    return usuarioRepository.findById(id).orElse(null);
  }

  @Override
  public void eliminarUsuario(Long id) {
    usuarioRepository.deleteById(id);
  }

  @Override
  public List<Asignatura> listarAsignaturas() {
    return asignaturaRepository.findAll();
  }

  @Override
  public Asignatura obtenerAsignatura(Long id) {
    return asignaturaRepository.findById(id).orElse(null);
  }

  @Override
  public Usuario crearUsuario(Usuario usuario, String rawPassword, Set<String> roles) {
    if (usuarioRepository.existsByEmail(usuario.getEmail())) {
      throw new IllegalArgumentException("Ya existe un usuario con ese email");
    }
    usuario.setPasswordHash(passwordEncoder.encode(rawPassword));

    if (roles != null && !roles.isEmpty()) {
      Set<Rol> resolved =
          roles.stream()
              .map(
                  r ->
                      rolRepository
                          .findTopByNombreIgnoreCaseOrderByIdAsc(r)
                          .orElseGet(
                              () -> {
                                Rol nr = new Rol();
                                nr.setNombre(r.toUpperCase());
                                nr.setDescripcion("Rol " + r.toUpperCase());
                                return rolRepository.save(nr);
                              }))
              .collect(Collectors.toSet());
      usuario.setRoles(resolved);
    }

    return usuarioRepository.save(usuario);
  }

  @Override
  public Asignatura crearAsignatura(Asignatura asignatura) {
    return asignaturaRepository.save(asignatura);
  }
}

