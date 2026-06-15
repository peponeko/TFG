package com.easy4you.repository;

import com.easy4you.model.entity.Documento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
  Optional<Documento> findByIdAndUsuarioId(Long id, Long usuarioId);

  List<Documento> findByUsuarioId(Long usuarioId);

  List<Documento> findByAsignaturaId(Long asignaturaId);

  List<Documento> findByTemaId(Long temaId);

  /** Evita N+1 y errores fuera de sesión al mapear a DTO sin OSIV (join solo ManyToOne, sin texto LONGTEXT). */
  @Query(
      """
      select distinct d from Documento d
      join fetch d.usuario
      join fetch d.asignatura
      left join fetch d.tema
      where d.asignatura.id = :asignaturaId
      """)
  List<Documento> findByAsignaturaIdFetchingRelations(@Param("asignaturaId") Long asignaturaId);

  @Query(
      """
      select distinct d from Documento d
      join fetch d.usuario
      join fetch d.asignatura
      left join fetch d.tema
      where d.tema.id = :temaId
      """)
  List<Documento> findByTemaIdFetchingRelations(@Param("temaId") Long temaId);

  @Query(
      """
      select distinct d from Documento d
      join fetch d.usuario
      join fetch d.asignatura
      left join fetch d.tema
      where d.usuario.id = :usuarioId
      """)
  List<Documento> findByUsuarioIdFetchingRelations(@Param("usuarioId") Long usuarioId);

  List<Documento> findByAsignaturaIdAndTemaId(Long asignaturaId, Long temaId);

  Optional<Documento> findTopByUsuarioIdAndChecksumSha256(Long usuarioId, String checksumSha256);

  @Query(
      """
      select d.tema.id, count(d.id)
      from Documento d
      where d.tema.id in :temaIds
      group by d.tema.id
      """)
  List<Object[]> countDocumentosByTemaIds(@Param("temaIds") List<Long> temaIds);
}
