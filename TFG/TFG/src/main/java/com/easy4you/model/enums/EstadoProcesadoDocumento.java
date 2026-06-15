package com.easy4you.model.enums;

public enum EstadoProcesadoDocumento {
  PENDIENTE,
  PROCESANDO,
  /**
   * Compatibilidad con datos existentes. Se considera equivalente a PROCESADO.
   * Preferir PROCESADO en nuevas escrituras.
   */
  LISTO,
  PROCESADO,
  ERROR
}
