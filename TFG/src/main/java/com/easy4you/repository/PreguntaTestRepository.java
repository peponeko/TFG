package com.easy4you.repository;

import com.easy4you.model.entity.PreguntaTest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreguntaTestRepository extends JpaRepository<PreguntaTest, Long> {
  List<PreguntaTest> findByDocumentoId(Long documentoId);

  List<PreguntaTest> findByTemaId(Long temaId);

  List<PreguntaTest> findByDocumentoIdIn(List<Long> documentoIds);

  Optional<PreguntaTest> findByIdAndUsuarioId(Long id, Long usuarioId);
}
