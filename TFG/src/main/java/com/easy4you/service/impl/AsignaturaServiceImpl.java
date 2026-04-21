package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.service.AsignaturaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AsignaturaServiceImpl implements AsignaturaService {

  private final AsignaturaRepository asignaturaRepository;

  @Override
  @Transactional(readOnly = true)
  public List<Asignatura> listar() {
    return asignaturaRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Asignatura> listarPorUsuarioId(Long usuarioId) {
    return asignaturaRepository.findByUsuarioIdOrderByNombreAsc(usuarioId);
  }

  @Override
  public Asignatura crear(Asignatura asignatura) {
    return asignaturaRepository.save(asignatura);
  }

  @Override
  @Transactional(readOnly = true)
  public Asignatura obtenerPorId(Long id) {
    return asignaturaRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + id));
  }

  @Override
  public Asignatura actualizar(Long id, Asignatura datos) {
    Asignatura existente = obtenerPorId(id);
    existente.setNombre(datos.getNombre());
    existente.setDescripcion(datos.getDescripcion());
    existente.setColorHex(datos.getColorHex());
    return asignaturaRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!asignaturaRepository.existsById(id)) {
      throw new NotFoundException("Asignatura no encontrada: " + id);
    }
    asignaturaRepository.deleteById(id);
  }
}
