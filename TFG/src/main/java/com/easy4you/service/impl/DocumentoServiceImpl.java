package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Documento;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.service.DocumentoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentoServiceImpl implements DocumentoService {

  private final DocumentoRepository documentoRepository;

  @Override
  @Transactional(readOnly = true)
  public List<Documento> listar() {
    return documentoRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Documento> listarPorUsuarioId(Long usuarioId) {
    return documentoRepository.findByUsuarioId(usuarioId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Documento> listarPorTemaId(Long temaId) {
    return documentoRepository.findByTemaId(temaId);
  }

  @Override
  public Documento crear(Documento documento) {
    return documentoRepository.save(documento);
  }

  @Override
  @Transactional(readOnly = true)
  public Documento obtenerPorId(Long id) {
    return documentoRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));
  }

  @Override
  public Documento actualizar(Long id, Documento datos) {
    Documento existente = obtenerPorId(id);
    existente.setNombreOriginal(datos.getNombreOriginal());
    existente.setRutaArchivo(datos.getRutaArchivo());
    existente.setMimeType(datos.getMimeType());
    existente.setExtension(datos.getExtension());
    existente.setTamanoBytes(datos.getTamanoBytes());
    existente.setChecksumSha256(datos.getChecksumSha256());
    existente.setTextoExtraido(datos.getTextoExtraido());
    existente.setPaginas(datos.getPaginas());
    existente.setEstadoProcesado(datos.getEstadoProcesado());
    existente.setErrorExtraccion(datos.getErrorExtraccion());

    if (datos.getTema() != null) {
      existente.setTema(datos.getTema());
    }

    return documentoRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!documentoRepository.existsById(id)) {
      throw new NotFoundException("Documento no encontrado: " + id);
    }
    documentoRepository.deleteById(id);
  }
}
