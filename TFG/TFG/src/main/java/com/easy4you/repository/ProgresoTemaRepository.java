package com.easy4you.repository;

import com.easy4you.model.entity.ProgresoTema;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgresoTemaRepository extends JpaRepository<ProgresoTema, Long> {
  Optional<ProgresoTema> findByUsuarioIdAndTemaId(Long usuarioId, Long temaId);

  List<ProgresoTema> findByUsuarioIdOrderByUpdatedAtDesc(Long usuarioId);

  @Query(
      """
      select pt
      from ProgresoTema pt
      join pt.tema t
      where pt.usuario.id = :usuarioId
        and t.asignatura.id = :asignaturaId
      """)
  List<ProgresoTema> findByUsuarioIdAndAsignaturaId(
      @Param("usuarioId") Long usuarioId, @Param("asignaturaId") Long asignaturaId);
}
