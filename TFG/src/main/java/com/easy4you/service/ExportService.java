package com.easy4you.service;

import org.springframework.stereotype.Service;

@Service
public class ExportService {

  public String exportNotebook(Long notebookId) {
    throw new UnsupportedOperationException(
        "P2 no implementado: exportación del notebook. TODO: exportar a PDF/ZIP incluyendo fuentes, "
            + "resúmenes, flashcards, tests y notas para notebookId="
            + notebookId);
  }
}

