package com.easy4you.repository;

import com.easy4you.model.entity.Flashcard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
  List<Flashcard> findByDocumentoId(Long documentoId);

  List<Flashcard> findByTemaId(Long temaId);

  List<Flashcard> findByDocumentoIdIn(List<Long> documentoIds);

  Optional<Flashcard> findByIdAndUsuarioId(Long id, Long usuarioId);
}
