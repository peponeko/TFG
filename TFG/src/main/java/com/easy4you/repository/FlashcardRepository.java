package com.easy4you.repository;

import com.easy4you.model.entity.Flashcard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
  List<Flashcard> findByDocumentoId(Long documentoId);

  List<Flashcard> findByTemaId(Long temaId);

  List<Flashcard> findByDocumentoIdIn(List<Long> documentoIds);

  Optional<Flashcard> findByIdAndUsuarioId(Long id, Long usuarioId);

  long countByDocumentoId(Long documentoId);

  @Query(
      """
      select f.tema.id, count(f.id)
      from Flashcard f
      where f.tema.id in :temaIds
      group by f.tema.id
      """)
  List<Object[]> countFlashcardsByTemaIds(@Param("temaIds") List<Long> temaIds);
}
