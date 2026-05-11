package com.easy4you.service.impl;

import com.easy4you.exception.NotFoundException;
import com.easy4you.dto.flashcard.FlashcardResponseDTO;
import com.easy4you.mapper.FlashcardMapper;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.Flashcard;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.service.FlashcardService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlashcardServiceImpl implements FlashcardService {

  private final FlashcardRepository flashcardRepository;
  private final DocumentoRepository documentoRepository;
  private final TemaRepository temaRepository;

  @Override
  public Flashcard crear(Flashcard flashcard) {
    return flashcardRepository.save(flashcard);
  }

  @Override
  @Transactional(readOnly = true)
  public Flashcard obtenerPorId(Long id) {
    return flashcardRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Flashcard no encontrada: " + id));
  }

  @Override
  public Flashcard actualizar(Long id, Flashcard datos) {
    Flashcard existente = obtenerPorId(id);
    existente.setPregunta(datos.getPregunta());
    existente.setRespuesta(datos.getRespuesta());
    if (datos.getDificultad() != null) {
      existente.setDificultad(datos.getDificultad());
    }
    if (datos.getDocumento() != null) {
      existente.setDocumento(datos.getDocumento());
    }
    return flashcardRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!flashcardRepository.existsById(id)) {
      throw new NotFoundException("Flashcard no encontrada: " + id);
    }
    flashcardRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<FlashcardResponseDTO> listarPorDocumento(Long usuarioId, Long documentoId) {
    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(documentoId, usuarioId)
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + documentoId));

    return flashcardRepository.findByDocumentoId(documento.getId()).stream()
        .filter(fc -> fc.getUsuario() != null && Objects.equals(fc.getUsuario().getId(), usuarioId))
        .map(FlashcardMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<FlashcardResponseDTO> listarPorTema(Long usuarioId, Long temaId) {
    if (temaRepository.findByIdAndAsignaturaUsuarioId(temaId, usuarioId).isEmpty()) {
      throw new NotFoundException("Tema no encontrado: " + temaId);
    }

    return flashcardRepository.findByTemaId(temaId).stream()
        .filter(fc -> fc.getUsuario() != null && Objects.equals(fc.getUsuario().getId(), usuarioId))
        .map(FlashcardMapper::toResponse)
        .toList();
  }
}
