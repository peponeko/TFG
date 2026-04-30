package com.easy4you.service.impl;

import com.easy4you.dto.pregunta.PreguntaTestResponseDTO;
import com.easy4you.dto.pregunta.ResponderPreguntaTestRequestDTO;
import com.easy4you.dto.pregunta.ResponderPreguntaTestResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.mapper.PreguntaTestMapper;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.PreguntaTest;
import com.easy4you.model.entity.PreguntaTestOpcion;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.PreguntaTestOpcionRepository;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.service.PreguntaTestService;
import com.easy4you.service.ProgresoUsuarioService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PreguntaTestServiceImpl implements PreguntaTestService {

  private final PreguntaTestRepository preguntaTestRepository;
  private final PreguntaTestOpcionRepository preguntaTestOpcionRepository;
  private final DocumentoRepository documentoRepository;
  private final TemaRepository temaRepository;
  private final ProgresoUsuarioService progresoUsuarioService;

  @Override
  public PreguntaTest crear(PreguntaTest preguntaTest) {
    return preguntaTestRepository.save(preguntaTest);
  }

  @Override
  @Transactional(readOnly = true)
  public PreguntaTest obtenerPorId(Long id) {
    return preguntaTestRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Pregunta test no encontrada: " + id));
  }

  @Override
  public PreguntaTest actualizar(Long id, PreguntaTest datos) {
    PreguntaTest existente = obtenerPorId(id);
    existente.setEnunciado(datos.getEnunciado());
    existente.setExplicacion(datos.getExplicacion());
    existente.setDificultad(datos.getDificultad());
    if (datos.getDocumento() != null) {
      existente.setDocumento(datos.getDocumento());
    }
    return preguntaTestRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!preguntaTestRepository.existsById(id)) {
      throw new NotFoundException("Pregunta test no encontrada: " + id);
    }
    preguntaTestRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PreguntaTestResponseDTO> listarPorDocumento(Long usuarioId, Long documentoId) {
    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(documentoId, usuarioId)
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + documentoId));

    return preguntaTestRepository.findByDocumentoId(documento.getId()).stream()
        .filter(p -> p.getUsuario() != null && Objects.equals(p.getUsuario().getId(), usuarioId))
        .map(
            p ->
                PreguntaTestMapper.toResponse(
                    p,
                    p.getId() == null
                        ? List.of()
                        : preguntaTestOpcionRepository.findByPreguntaTestIdOrderByOrdenAsc(p.getId())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PreguntaTestResponseDTO> listarPorTema(Long usuarioId, Long temaId) {
    if (temaRepository.findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(temaId, usuarioId).isEmpty()) {
      throw new NotFoundException("Tema no encontrado: " + temaId);
    }

    return preguntaTestRepository.findByTemaId(temaId).stream()
        .filter(p -> p.getUsuario() != null && Objects.equals(p.getUsuario().getId(), usuarioId))
        .map(
            p ->
                PreguntaTestMapper.toResponse(
                    p,
                    p.getId() == null
                        ? List.of()
                        : preguntaTestOpcionRepository.findByPreguntaTestIdOrderByOrdenAsc(p.getId())))
        .toList();
  }

  @Override
  public ResponderPreguntaTestResponseDTO responder(
      Long usuarioId, Long preguntaTestId, ResponderPreguntaTestRequestDTO request) {

    PreguntaTest pregunta =
        preguntaTestRepository
            .findById(preguntaTestId)
            .orElseThrow(() -> new NotFoundException("Pregunta test no encontrada: " + preguntaTestId));

    if (pregunta.getUsuario() == null || !usuarioId.equals(pregunta.getUsuario().getId())) {
      throw new NotFoundException("Pregunta test no encontrada: " + preguntaTestId);
    }

    List<PreguntaTestOpcion> opciones =
        preguntaTestOpcionRepository.findByPreguntaTestIdOrderByOrdenAsc(pregunta.getId());
    if (opciones.size() < 4) {
      throw new BadRequestException("La pregunta no tiene opciones suficientes");
    }

    int indiceCorrecto = -1;
    for (int i = 0; i < opciones.size(); i++) {
      if (opciones.get(i).isEsCorrecta()) {
        indiceCorrecto = i;
        break;
      }
    }

    if (indiceCorrecto < 0) {
      throw new BadRequestException("La pregunta no tiene respuesta correcta configurada");
    }

    boolean correcta = Objects.equals(indiceCorrecto, request.getIndiceOpcion());
    progresoUsuarioService.registrarRespuestaTest(usuarioId, pregunta.getId(), correcta);

    return new ResponderPreguntaTestResponseDTO(correcta, indiceCorrecto, pregunta.getExplicacion());
  }
}
