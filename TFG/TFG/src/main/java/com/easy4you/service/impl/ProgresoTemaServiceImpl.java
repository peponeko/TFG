package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.ProgresoTema;
import com.easy4you.repository.ProgresoTemaRepository;
import com.easy4you.service.ProgresoTemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgresoTemaServiceImpl implements ProgresoTemaService {

  private final ProgresoTemaRepository progresoTemaRepository;

  @Override
  public ProgresoTema crear(ProgresoTema progresoTema) {
    return progresoTemaRepository.save(progresoTema);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgresoTema obtenerPorId(Long id) {
    return progresoTemaRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Progreso por tema no encontrado: " + id));
  }

  @Override
  public ProgresoTema actualizar(Long id, ProgresoTema datos) {
    ProgresoTema existente = obtenerPorId(id);
    existente.setPorcentaje(datos.getPorcentaje());
    existente.setSesionesCompletadas(datos.getSesionesCompletadas());
    existente.setMinutosEstudiados(datos.getMinutosEstudiados());
    existente.setUltimaSesion(datos.getUltimaSesion());
    return progresoTemaRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!progresoTemaRepository.existsById(id)) {
      throw new NotFoundException("Progreso por tema no encontrado: " + id);
    }
    progresoTemaRepository.deleteById(id);
  }
}

