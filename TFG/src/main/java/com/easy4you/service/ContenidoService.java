package com.easy4you.service;

import com.easy4you.dto.contenido.FlashcardGeneradaDTO;
import com.easy4you.dto.contenido.PreguntaTestGeneradaDTO;
import java.util.List;

public interface ContenidoService {

  String generarResumen(String texto);

  List<PreguntaTestGeneradaDTO> generarPreguntasTest(String texto);

  List<FlashcardGeneradaDTO> generarFlashcards(String texto);
}

