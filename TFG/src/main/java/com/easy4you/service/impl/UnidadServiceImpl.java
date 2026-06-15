package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Unidad;
import com.easy4you.repository.UnidadRepository;
import com.easy4you.service.UnidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UnidadServiceImpl implements UnidadService {

  private final UnidadRepository unidadRepository;

  @Override
  public Unidad crear(Unidad unidad) {
    return unidadRepository.save(unidad);
  }

  @Override
  @Transactional(readOnly = true)
  public Unidad obtenerPorId(Long id) {
    return unidadRepository.findById(id).orElseThrow(() -> new NotFoundException("Unidad no encontrada: " + id));
  }

  @Override
  public Unidad actualizar(Long id, Unidad datos) {
    Unidad existente = obtenerPorId(id);
    existente.setTitulo(datos.getTitulo());
    existente.setDescripcion(datos.getDescripcion());
    existente.setOrden(datos.getOrden());
    return unidadRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!unidadRepository.existsById(id)) {
      throw new NotFoundException("Unidad no encontrada: " + id);
    }
    unidadRepository.deleteById(id);
  }
}

