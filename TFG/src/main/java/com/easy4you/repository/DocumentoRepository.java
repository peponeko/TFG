package com.easy4you.repository;

import com.easy4you.model.entity.Documento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
  Optional<Documento> findByIdAndUsuarioId(Long id, Long usuarioId);

  List<Documento> findByUsuarioId(Long usuarioId);

  List<Documento> findByAsignaturaId(Long asignaturaId);

  List<Documento> findByTemaId(Long temaId);

  Optional<Documento> findTopByUsuarioIdAndChecksumSha256(Long usuarioId, String checksumSha256);
}
