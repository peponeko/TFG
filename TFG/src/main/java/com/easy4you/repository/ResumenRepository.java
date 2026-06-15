package com.easy4you.repository;

import com.easy4you.model.entity.Resumen;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumenRepository extends JpaRepository<Resumen, Long> {
  Optional<Resumen> findByIdAndUsuarioId(Long id, Long usuarioId);

  List<Resumen> findByDocumentoIdOrderByCreatedAtDesc(Long documentoId);

  List<Resumen> findByTemaIdOrderByCreatedAtDesc(Long temaId);

  List<Resumen> findByDocumentoIdInOrderByCreatedAtDesc(List<Long> documentoIds);

  long countByDocumentoId(Long documentoId);
}
