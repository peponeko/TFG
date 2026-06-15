package com.easy4you.repository;

import com.easy4you.model.entity.EventoCalendario;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoCalendarioRepository extends JpaRepository<EventoCalendario, Long> {

  List<EventoCalendario> findByUsuarioIdOrderByFechaInicioAsc(Long usuarioId);

  List<EventoCalendario> findByUsuarioIdAndFechaInicioBetweenOrderByFechaInicioAsc(
      Long usuarioId, LocalDate desde, LocalDate hasta);

  Optional<EventoCalendario> findByIdAndUsuarioId(Long id, Long usuarioId);
}

