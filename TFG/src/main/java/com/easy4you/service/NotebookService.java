package com.easy4you.service;

import com.easy4you.dto.notebook.NotebookOverviewResponseDTO;

public interface NotebookService {
  NotebookOverviewResponseDTO obtenerOverview(Long usuarioId, Long notebookId, Long temaId);
}

