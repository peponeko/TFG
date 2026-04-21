package com.easy4you.repository;

import com.easy4you.model.entity.Tema;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemaRepository extends JpaRepository<Tema, Long> {
  List<Tema> findByUnidadIdOrderByOrdenAsc(Long unidadId);

  List<Tema> findByUnidadResultadoAprendizajeAsignaturaIdOrderByOrdenAsc(Long asignaturaId);

  Optional<Tema> findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(Long id, Long usuarioId);
}
