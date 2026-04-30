package com.easy4you.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AudioOverviewService {

  private static final Logger log = LoggerFactory.getLogger(AudioOverviewService.class);

  public String generateAudioOverview(Long notebookId) {
    log.info("AudioOverviewService no implementado aún - trabajo futuro (notebookId={})", notebookId);
    return "Funcionalidad pendiente de implementacion";
  }
}
