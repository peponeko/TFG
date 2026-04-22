package com.easy4you.repository;

import com.easy4you.model.entity.Tema;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemaRepository extends JpaRepository<Tema, Long> {
  List<Tema> findByUnidadIdOrderByOrdenAsc(Long unidadId);

  List<Tema> findByUnidadResultadoAprendizajeAsignaturaIdOrderByOrdenAsc(Long asignaturaId);

  Optional<Tema> findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(Long id, Long usuarioId);

  @Query(
      """
      select ra.codigo, count(t.id)
      from Tema t
      join t.unidad u
      join u.resultadoAprendizaje ra
      where ra.asignatura.id = :asignaturaId
      group by ra.codigo
      """)
  List<Object[]> countTemasByAsignaturaIdGroupByResultadoCodigo(@Param("asignaturaId") Long asignaturaId);

  @Query(
      """
      select t.id, t.titulo, t.descripcion, t.palabrasClave, ra.codigo
      from Tema t
      join t.unidad u
      join u.resultadoAprendizaje ra
      where ra.asignatura.id = :asignaturaId
      order by t.titulo asc, t.id asc
      """)
  List<Object[]> findTemasPlanosByAsignaturaId(@Param("asignaturaId") Long asignaturaId);
}
