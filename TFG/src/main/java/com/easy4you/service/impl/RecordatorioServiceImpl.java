package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Recordatorio;
import com.easy4you.repository.RecordatorioRepository;
import com.easy4you.service.RecordatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordatorioServiceImpl implements RecordatorioService {

  private final RecordatorioRepository recordatorioRepository;

  @Override
  public Recordatorio crear(Recordatorio recordatorio) {
    return recordatorioRepository.save(recordatorio);
  }

  @Override
  @Transactional(readOnly = true)
  public Recordatorio obtenerPorId(Long id) {
    return recordatorioRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Recordatorio no encontrado: " + id));
  }

  @Override
  public Recordatorio actualizar(Long id, Recordatorio datos) {
    Recordatorio existente = obtenerPorId(id);
    existente.setTitulo(datos.getTitulo());
    existente.setMensaje(datos.getMensaje());
    existente.setFechaHora(datos.getFechaHora());
    existente.setEstado(datos.getEstado());
    return recordatorioRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!recordatorioRepository.existsById(id)) {
      throw new NotFoundException("Recordatorio no encontrado: " + id);
    }
    recordatorioRepository.deleteById(id);
  }
}

