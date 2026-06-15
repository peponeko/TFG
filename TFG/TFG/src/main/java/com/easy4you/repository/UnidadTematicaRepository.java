package com.easy4you.repository;

import com.easy4you.model.entity.UnidadTematica;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadTematicaRepository extends JpaRepository<UnidadTematica, Long> {

  List<UnidadTematica> findByAsignaturaIdOrderByOrdenAsc(Long asignaturaId);

  Optional<UnidadTematica> findByIdAndAsignaturaUsuarioId(Long id, Long usuarioId);
}

