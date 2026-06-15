package com.easy4you.repository;

import com.easy4you.model.entity.Unidad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadRepository extends JpaRepository<Unidad, Long> {
  List<Unidad> findByResultadoAprendizajeIdOrderByOrdenAsc(Long resultadoAprendizajeId);

  boolean existsByIdAndResultadoAprendizajeAsignaturaUsuarioId(Long id, Long usuarioId);
}
