package com.easy4you.repository;

import com.easy4you.model.entity.Tema;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemaRepository extends JpaRepository<Tema, Long> {
  List<Tema> findByAsignaturaIdOrderByOrdenAsc(Long asignaturaId);

  Optional<Tema> findByIdAndAsignaturaUsuarioId(Long id, Long usuarioId);

  Optional<Tema> findByIdAndAsignaturaId(Long id, Long asignaturaId);

  @Query(
      """
      select t.trimestre, count(t.id)
      from Tema t
      where t.asignatura.id = :asignaturaId
      group by t.trimestre
      """)
  List<Object[]> countTemasByAsignaturaIdGroupByTrimestre(@Param("asignaturaId") Long asignaturaId);

  @Query(
      """
      select t.id, t.titulo, t.descripcion, t.palabrasClave, t.unidadTematica.id, t.trimestre
      from Tema t
      where t.asignatura.id = :asignaturaId
      order by t.titulo asc, t.id asc
      """)
  List<Object[]> findTemasPlanosByAsignaturaId(@Param("asignaturaId") Long asignaturaId);
}
