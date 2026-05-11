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
      where n.usuario.id = :usuarioId
        and (:documentoId is null or d.id = :documentoId)
        and (:temaId is null or t.id = :temaId)
        and (
          :asignaturaId is null
          or (d is not null and d.asignatura.id = :asignaturaId)
          or (t is not null and t.asignatura.id = :asignaturaId)
        )
      order by n.updatedAt desc
      """)
  List<Nota> search(
      @Param("usuarioId") Long usuarioId,
      @Param("documentoId") Long documentoId,
      @Param("temaId") Long temaId,
      @Param("asignaturaId") Long asignaturaId);
}
