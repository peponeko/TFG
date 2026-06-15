package com.easy4you.service;

import com.easy4you.dto.pregunta.PreguntaTestResponseDTO;
import com.easy4you.dto.pregunta.ResponderPreguntaTestRequestDTO;
import com.easy4you.dto.pregunta.ResponderPreguntaTestResponseDTO;
import com.easy4you.model.entity.PreguntaTest;
import java.util.List;

public interface PreguntaTestService {
  PreguntaTest crear(PreguntaTest preguntaTest);

  PreguntaTest obtenerPorId(Long id);

  PreguntaTest actualizar(Long id, PreguntaTest preguntaTest);

  void eliminar(Long id);

  List<PreguntaTestResponseDTO> listarPorDocumento(Long usuarioId, Long documentoId);

  List<PreguntaTestResponseDTO> listarPorTema(Long usuarioId, Long temaId);

  ResponderPreguntaTestResponseDTO responder(
      Long usuarioId, Long preguntaTestId, ResponderPreguntaTestRequestDTO request);
}
