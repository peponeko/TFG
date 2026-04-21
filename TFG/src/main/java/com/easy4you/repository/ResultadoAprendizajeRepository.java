package com.easy4you.repository;

import com.easy4you.model.entity.ResultadoAprendizaje;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoAprendizajeRepository extends JpaRepository<ResultadoAprendizaje, Long> {
  List<ResultadoAprendizaje> findByAsignaturaIdOrderByOrdenAsc(Long asignaturaId);
}

