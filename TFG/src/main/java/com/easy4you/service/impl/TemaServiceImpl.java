package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Tema;
import com.easy4you.repository.TemaRepository;
import com.easy4you.service.TemaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TemaServiceImpl implements TemaService {

  private final TemaRepository temaRepository;

  @Override
  @Transactional(readOnly = true)
  public List<Tema> listar() {
    return temaRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Tema> listarPorUnidadId(Long unidadId) {
    return temaRepository.findByUnidadIdOrderByOrdenAsc(unidadId);
  }

  @Override
  public Tema crear(Tema tema) {
    return temaRepository.save(tema);
  }

  @Override
  @Transactional(readOnly = true)
  public Tema obtenerPorId(Long id) {
    return temaRepository.findById(id).orElseThrow(() -> new NotFoundException("Tema no encontrado: " + id));
  }

  @Override
  public Tema actualizar(Long id, Tema datos) {
    Tema existente = obtenerPorId(id);
    if (datos.getUnidad() != null) {
      existente.setUnidad(datos.getUnidad());
    }
    existente.setTitulo(datos.getTitulo());
    existente.setDescripcion(datos.getDescripcion());
    existente.setOrden(datos.getOrden());
    existente.setPalabrasClave(datos.getPalabrasClave());
    return temaRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!temaRepository.existsById(id)) {
      throw new NotFoundException("Tema no encontrado: " + id);
    }
    temaRepository.deleteById(id);
  }
}
