package com.easy4you.repository;

import com.easy4you.model.entity.Recordatorio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordatorioRepository extends JpaRepository<Recordatorio, Long> {
  List<Recordatorio> findByUsuarioIdOrderByFechaHoraAsc(Long usuarioId);
}

