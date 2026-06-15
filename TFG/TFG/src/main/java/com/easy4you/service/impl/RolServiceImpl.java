package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Rol;
import com.easy4you.repository.RolRepository;
import com.easy4you.service.RolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RolServiceImpl implements RolService {

  private final RolRepository rolRepository;

  @Override
  public Rol crear(Rol rol) {
    return rolRepository.save(rol);
  }

  @Override
  @Transactional(readOnly = true)
  public Rol obtenerPorId(Long id) {
    return rolRepository.findById(id).orElseThrow(() -> new NotFoundException("Rol no encontrado: " + id));
  }

  @Override
  public Rol actualizar(Long id, Rol datos) {
    Rol existente = obtenerPorId(id);
    existente.setNombre(datos.getNombre());
    existente.setDescripcion(datos.getDescripcion());
    return rolRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!rolRepository.existsById(id)) {
      throw new NotFoundException("Rol no encontrado: " + id);
    }
    rolRepository.deleteById(id);
  }
}

