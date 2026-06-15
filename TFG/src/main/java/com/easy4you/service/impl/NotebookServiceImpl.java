package com.easy4you.service.impl;

import com.easy4you.dto.documento.DocumentoResponseDTO;
import com.easy4you.dto.flashcard.FlashcardResponseDTO;
import com.easy4you.dto.nota.NotaResponseDTO;
import com.easy4you.dto.notebook.NotebookOverviewResponseDTO;
import com.easy4you.dto.pregunta.PreguntaTestResponseDTO;
import com.easy4you.dto.resumen.ResumenResponseDTO;
import com.easy4you.exception.NotFoundException;
import com.easy4you.mapper.AsignaturaMapper;
import com.easy4you.mapper.DocumentoMapper;
import com.easy4you.mapper.FlashcardMapper;
import com.easy4you.mapper.PreguntaTestMapper;
import com.easy4you.mapper.ResumenMapper;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.Nota;
import com.easy4you.model.enums.RolNotebookCompartido;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.repository.NotaRepository;
import com.easy4you.repository.PreguntaTestOpcionRepository;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.repository.ResumenRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.service.NotebookCompartidoService;
import com.easy4you.service.NotebookService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotebookServiceImpl implements NotebookService {

  private final AsignaturaRepository asignaturaRepository;
  private final DocumentoRepository documentoRepository;
  private final ResumenRepository resumenRepository;
  private final FlashcardRepository flashcardRepository;
  private final PreguntaTestRepository preguntaTestRepository;
  private final PreguntaTestOpcionRepository preguntaTestOpcionRepository;
  private final NotaRepository notaRepository;
  private final TemaRepository temaRepository;
  private final NotebookCompartidoService notebookCompartidoService;

  @Override
  @Transactional(readOnly = true)
  public NotebookOverviewResponseDTO obtenerOverview(Long usuarioId, Long notebookId, Long temaId) {
    // Verificar acceso (propietario o invitado)
    if (!notebookCompartidoService.tieneAcceso(usuarioId, notebookId)) {
      throw new NotFoundException("Notebook no encontrado o sin acceso: " + notebookId);
    }

    // Obtener rol del usuario (por ahora no afecta a la respuesta)
    notebookCompartidoService.obtenerRol(usuarioId, notebookId).orElse(RolNotebookCompartido.VIEWER);

    // Obtener asignatura (puede ser propietario o compartida)
    Asignatura asignatura =
        asignaturaRepository
            .findById(notebookId)
            .orElseThrow(() -> new NotFoundException("Notebook no encontrado: " + notebookId));

    if (temaId != null && temaRepository.findByIdAndAsignaturaId(temaId, asignatura.getId()).isEmpty()) {
      throw new NotFoundException("Tema no encontrado en este notebook: " + temaId);
    }

    // Documentos de la asignatura (o del tema si se filtra)
    List<Documento> documentos =
        temaId == null
            ? documentoRepository.findByAsignaturaId(asignatura.getId())
            : documentoRepository.findByAsignaturaIdAndTemaId(asignatura.getId(), temaId);

    List<Long> documentoIds = documentos.stream().map(Documento::getId).filter(Objects::nonNull).toList();

    // Resúmenes de los documentos
    List<ResumenResponseDTO> resumenes =
        documentoIds.isEmpty()
            ? List.of()
            : resumenRepository.findByDocumentoIdInOrderByCreatedAtDesc(documentoIds).stream()
                .map(ResumenMapper::toResponse)
                .toList();

    // Flashcards de los documentos
    List<FlashcardResponseDTO> flashcards =
        documentoIds.isEmpty()
            ? List.of()
            : flashcardRepository.findByDocumentoIdIn(documentoIds).stream()
                .map(FlashcardMapper::toResponse)
                .toList();

    // Preguntas de los documentos
    List<PreguntaTestResponseDTO> preguntas =
        documentoIds.isEmpty()
            ? List.of()
            : preguntaTestRepository.findByDocumentoIdIn(documentoIds).stream()
                .map(
                    p ->
                        PreguntaTestMapper.toResponse(
                            p,
                            p.getId() == null
                                ? List.of()
                                : preguntaTestOpcionRepository.findByPreguntaTestIdOrderByOrdenAsc(p.getId())))
                .toList();

    // Notas del usuario en esta asignatura
    List<NotaResponseDTO> notas =
        notaRepository.search(usuarioId, null, temaId, asignatura.getId()).stream()
            .map(this::toNotaResponse)
            .toList();

    List<DocumentoResponseDTO> documentosDto = documentos.stream().map(DocumentoMapper::toResponse).toList();

    NotebookOverviewResponseDTO response =
        new NotebookOverviewResponseDTO(
            AsignaturaMapper.toResponse(asignatura), documentosDto, resumenes, flashcards, preguntas, notas);

    return response;
  }

  private NotaResponseDTO toNotaResponse(Nota nota) {
    return new NotaResponseDTO(
        nota.getId(),
        nota.getUsuario() != null ? nota.getUsuario().getId() : null,
        nota.getDocumento() != null ? nota.getDocumento().getId() : null,
        nota.getChunk() != null ? nota.getChunk().getId() : null,
        nota.getTema() != null ? nota.getTema().getId() : null,
        nota.getTitulo(),
        nota.getContenido(),
        nota.getColorHex(),
        nota.getCreatedAt(),
        nota.getUpdatedAt());
  }
}
