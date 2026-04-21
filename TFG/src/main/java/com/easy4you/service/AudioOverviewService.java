package com.easy4you.service;

import org.springframework.stereotype.Service;

@Service
public class AudioOverviewService {

  public String generateAudioOverview(Long notebookId) {
    throw new UnsupportedOperationException(
        "P2 no implementado: generación de Audio Overview. TODO: implementar pipeline TTS + "
            + "síntesis basada en artefactos del notebookId="
            + notebookId);
  }
}

