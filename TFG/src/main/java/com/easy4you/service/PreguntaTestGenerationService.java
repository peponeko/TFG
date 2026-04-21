package com.easy4you.service;

import com.easy4you.model.entity.PreguntaTest;
import java.util.List;

public interface PreguntaTestGenerationService {
  List<PreguntaTest> generarParaDocumento(Long usuarioId, Long documentoId);
}

