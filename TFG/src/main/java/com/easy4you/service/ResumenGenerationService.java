package com.easy4you.service;

import com.easy4you.model.entity.ArtefactoGenerado;
import com.easy4you.model.entity.Resumen;

public interface ResumenGenerationService {
  Resumen generarResumenDocumento(Long usuarioId, Long documentoId);

  Resumen generarResumenTema(Long usuarioId, Long temaId);

  /**
   * Genera resumen y registra el artefacto generado
   */
  ArtefactoGenerado generarResumenConArtefacto(Long usuarioId, Long documentoId);
}

