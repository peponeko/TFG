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
