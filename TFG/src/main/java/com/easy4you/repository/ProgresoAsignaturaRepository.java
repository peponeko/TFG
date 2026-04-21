package com.easy4you.repository;

import com.easy4you.model.entity.ProgresoAsignatura;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgresoAsignaturaRepository extends JpaRepository<ProgresoAsignatura, Long> {
  Optional<ProgresoAsignatura> findByUsuarioIdAndAsignaturaId(Long usuarioId, Long asignaturaId);
}

