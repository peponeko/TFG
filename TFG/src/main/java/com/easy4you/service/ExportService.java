package com.easy4you.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

  private static final Logger log = LoggerFactory.getLogger(ExportService.class);

  public String exportNotebook(Long notebookId) {
    log.info("ExportService no implementado aún - trabajo futuro (notebookId={})", notebookId);
    return "Funcionalidad pendiente de implementacion";
  }
}
