package com.easy4you.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VideoOverviewService {

  private static final Logger log = LoggerFactory.getLogger(VideoOverviewService.class);

  public String generateVideoOverview(Long notebookId) {
    log.info("VideoOverviewService no implementado aún - trabajo futuro (notebookId={})", notebookId);
    return "Funcionalidad pendiente de implementacion";
  }
}
