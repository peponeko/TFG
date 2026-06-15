package com.easy4you.repository;

import com.easy4you.model.entity.ArtefactoGenerado;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtefactoGeneradoRepository extends JpaRepository<ArtefactoGenerado, Long> {
  List<ArtefactoGenerado> findByAsignaturaIdOrderByCreatedAtDesc(Long asignaturaId);
}

