package com.easy4you.service.impl;

import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Nota;
import com.easy4you.repository.NotaRepository;
import com.easy4you.service.NotaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotaServiceImpl implements NotaService {

  private final NotaRepository notaRepository;

  @Override
  public Nota crear(Nota nota) {
    if (nota == null) {
      throw new BadRequestException("La nota es obligatoria");
    }
    if (nota.getUsuario() == null || nota.getUsuario().getId() == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    return notaRepository.save(nota);
  }

  @Override
  @Transactional(readOnly = true)
  public Nota obtenerPorId(Long usuarioId, Long id) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (id == null) {
      throw new BadRequestException("id es obligatorio");
    }

    return notaRepository
        .findByIdAndUsuarioId(id, usuarioId)
        .orElseThrow(() -> new NotFoundException("Nota no encontrada: " + id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Nota> listar(Long usuarioId, Long documentoId, Long temaId, Long asignaturaId) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }

    return notaRepository.search(usuarioId, documentoId, temaId, asignaturaId);
  }

  @Override
  public Nota actualizar(Long usuarioId, Long id, Nota datos) {
    Nota existente = obtenerPorId(usuarioId, id);

    existente.setTitulo(datos.getTitulo());
    existente.setContenido(datos.getContenido());
    existente.setColorHex(datos.getColorHex());

    return notaRepository.save(existente);
  }

  @Override
  public void eliminar(Long usuarioId, Long id) {
    Nota existente = obtenerPorId(usuarioId, id);
    notaRepository.delete(existente);
  }
}

