package com.easy4you.repository;

import com.easy4you.model.entity.DocumentoChunk;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoChunkRepository extends JpaRepository<DocumentoChunk, Long> {
  void deleteByDocumentoId(Long documentoId);

  Optional<DocumentoChunk> findByIdAndDocumentoUsuarioId(Long id, Long usuarioId);

  Page<DocumentoChunk> findByDocumentoIdOrderByIndiceChunkAsc(Long documentoId, Pageable pageable);

  Page<DocumentoChunk> findByDocumentoIdInAndTextoContainingIgnoreCase(
      List<Long> documentoIds, String texto, Pageable pageable);

  Page<DocumentoChunk> findByDocumentoIdInOrderByDocumentoIdAscIndiceChunkAsc(
      List<Long> documentoIds, Pageable pageable);

  /**
   * Búsqueda FULLTEXT en MySQL con ranking por relevancia.
   * Usa MATCH ... AGAINST en modo natural language.
   * Ordena por SCORE de relevancia (desc) y luego por índice de chunk.
   */
  @Query(value = """
      SELECT dc.*, MATCH(dc.texto) AGAINST(:query IN NATURAL LANGUAGE MODE) AS relevance_score
      FROM documento_chunk dc
      WHERE dc.documento_id IN :documentoIds
        AND MATCH(dc.texto) AGAINST(:query IN NATURAL LANGUAGE MODE)
      ORDER BY relevance_score DESC, dc.indice_chunk ASC
      """, nativeQuery = true)
  Page<DocumentoChunk> searchFullText(
      @Param("documentoIds") List<Long> documentoIds,
      @Param("query") String query,
      Pageable pageable);

  /**
   * Búsqueda FULLTEXT sin filtro de documentos (para búsquedas globales).
   */
  @Query(value = """
      SELECT dc.*, MATCH(dc.texto) AGAINST(:query IN NATURAL LANGUAGE MODE) AS relevance_score
      FROM documento_chunk dc
      WHERE MATCH(dc.texto) AGAINST(:query IN NATURAL LANGUAGE MODE)
      ORDER BY relevance_score DESC, dc.indice_chunk ASC
      """, nativeQuery = true)
  Page<DocumentoChunk> searchFullTextGlobal(
      @Param("query") String query,
      Pageable pageable);
}
