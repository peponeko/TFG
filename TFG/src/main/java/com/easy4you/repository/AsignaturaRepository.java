package com.easy4you.repository;

import com.easy4you.model.entity.Asignatura;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {
  Optional<Asignatura> findByIdAndUsuarioId(Long id, Long usuarioId);

  List<Asignatura> findByUsuarioIdOrderByNombreAsc(Long usuarioId);
}
