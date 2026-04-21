package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Resumen;
import com.easy4you.repository.ResumenRepository;
import com.easy4you.service.ResumenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumenServiceImpl implements ResumenService {

  private final ResumenRepository resumenRepository;

  @Override
  public Resumen crear(Resumen resumen) {
    return resumenRepository.save(resumen);
  }

  @Override
  @Transactional(readOnly = true)
  public Resumen obtenerPorId(Long id) {
    return resumenRepository.findById(id).orElseThrow(() -> new NotFoundException("Resumen no encontrado: " + id));
  }

  @Override
  public Resumen actualizar(Long id, Resumen datos) {
    Resumen existente = obtenerPorId(id);
    existente.setTitulo(datos.getTitulo());
    existente.setContenido(datos.getContenido());
    existente.setOrigen(datos.getOrigen());
    if (datos.getDocumento() != null) {
      existente.setDocumento(datos.getDocumento());
    }
    return resumenRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!resumenRepository.existsById(id)) {
      throw new NotFoundException("Resumen no encontrado: " + id);
    }
    resumenRepository.deleteById(id);
  }
}

