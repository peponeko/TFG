package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.Resumen;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.ResumenRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.service.ResumenService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumenServiceImpl implements ResumenService {

  private final ResumenRepository resumenRepository;
  private final DocumentoRepository documentoRepository;
  private final TemaRepository temaRepository;

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

  @Override
  @Transactional(readOnly = true)
  public List<Resumen> listarPorDocumento(Long usuarioId, Long documentoId) {
    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(documentoId, usuarioId)
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + documentoId));

    return resumenRepository.findByDocumentoIdOrderByCreatedAtDesc(documento.getId()).stream()
        .filter(r -> r.getUsuario() != null && Objects.equals(r.getUsuario().getId(), usuarioId))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Resumen> listarPorTema(Long usuarioId, Long temaId) {
    if (temaRepository.findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(temaId, usuarioId).isEmpty()) {
      throw new NotFoundException("Tema no encontrado: " + temaId);
    }

    return resumenRepository.findByTemaIdOrderByCreatedAtDesc(temaId).stream()
        .filter(r -> r.getUsuario() != null && Objects.equals(r.getUsuario().getId(), usuarioId))
        .toList();
  }
}
