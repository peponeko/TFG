package com.easy4you.service;

import com.easy4you.model.entity.Recordatorio;

public interface RecordatorioService {
  Recordatorio crear(Recordatorio recordatorio);

  Recordatorio obtenerPorId(Long id);

  Recordatorio actualizar(Long id, Recordatorio recordatorio);

  void eliminar(Long id);
}

