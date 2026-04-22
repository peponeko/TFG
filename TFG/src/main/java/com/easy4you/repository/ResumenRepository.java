package com.easy4you.repository;

import com.easy4you.model.entity.Resumen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumenRepository extends JpaRepository<Resumen, Long> {
  List<Resumen> findByDocumentoIdOrderByCreatedAtDesc(Long documentoId);

  List<Resumen> findByTemaIdOrderByCreatedAtDesc(Long temaId);

  List<Resumen> findByDocumentoIdInOrderByCreatedAtDesc(List<Long> documentoIds);

  long countByDocumentoId(Long documentoId);
}
