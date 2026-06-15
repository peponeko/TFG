package com.easy4you.service;

import com.easy4you.dto.progreso.ProgresoTemaResponseDTO;
import com.easy4you.dto.progreso.ProgresoUsuarioResponseDTO;

public interface ProgresoUsuarioService {
  ProgresoUsuarioResponseDTO obtenerProgresoUsuario(Long usuarioId);

  ProgresoTemaResponseDTO obtenerProgresoTema(Long usuarioId, Long temaId);

  void registrarRespuestaTest(Long usuarioId, Long preguntaTestId, boolean correcta);

  void registrarRepasoFlashcard(Long usuarioId, Long flashcardId);
}

