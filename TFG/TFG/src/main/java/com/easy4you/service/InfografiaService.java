package com.easy4you.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InfografiaService {

  private static final Logger log = LoggerFactory.getLogger(InfografiaService.class);

  public String generateInfografia(Long notebookId) {
    log.info("InfografiaService no implementado aún - trabajo futuro (notebookId={})", notebookId);
    return "Funcionalidad pendiente de implementacion";
  }
}
