package com.easy4you.repository;

import com.easy4you.model.entity.Nota;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotaRepository extends JpaRepository<Nota, Long> {
  List<Nota> findByUsuarioIdOrderByUpdatedAtDesc(Long usuarioId);

  Optional<Nota> findByIdAndUsuarioId(Long id, Long usuarioId);

  @Query(
      """
      select n
      from Nota n
      left join n.documento d
      left join n.tema t
      left join t.unidad u
      left join u.resultadoAprendizaje ra
      left join ra.asignatura asigTema
      where n.usuario.id = :usuarioId
        and (:documentoId is null or d.id = :documentoId)
        and (:temaId is null or t.id = :temaId)
        and (
          :asignaturaId is null
          or (d is not null and d.asignatura.id = :asignaturaId)
          or (asigTema is not null and asigTema.id = :asignaturaId)
        )
      order by n.updatedAt desc
      """)
  List<Nota> search(
      @Param("usuarioId") Long usuarioId,
      @Param("documentoId") Long documentoId,
      @Param("temaId") Long temaId,
      @Param("asignaturaId") Long asignaturaId);
}
