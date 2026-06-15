package com.easy4you.repository;

import com.easy4you.model.entity.PreguntaTest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreguntaTestRepository extends JpaRepository<PreguntaTest, Long> {
  List<PreguntaTest> findByDocumentoId(Long documentoId);

  List<PreguntaTest> findByTemaId(Long temaId);

  List<PreguntaTest> findByDocumentoIdIn(List<Long> documentoIds);

  Optional<PreguntaTest> findByIdAndUsuarioId(Long id, Long usuarioId);

  long countByDocumentoId(Long documentoId);

  @Query(
      """
      select p.tema.id, count(p.id)
      from PreguntaTest p
      where p.tema.id in :temaIds
      group by p.tema.id
      """)
  List<Object[]> countPreguntasByTemaIds(@Param("temaIds") List<Long> temaIds);
}
