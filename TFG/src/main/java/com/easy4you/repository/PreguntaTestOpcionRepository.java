package com.easy4you.repository;

import com.easy4you.model.entity.PreguntaTestOpcion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreguntaTestOpcionRepository extends JpaRepository<PreguntaTestOpcion, Long> {
  List<PreguntaTestOpcion> findByPreguntaTestIdOrderByOrdenAsc(Long preguntaTestId);
}

