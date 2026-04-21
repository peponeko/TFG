package com.easy4you.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(
    name = "documento_chunk",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_documento_chunk_documento_indice",
          columnNames = {"documento_id", "indice_chunk"})
    },
    indexes = {
      @Index(name = "idx_documento_chunk_documento", columnList = "documento_id"),
      @Index(name = "idx_documento_chunk_documento_indice", columnList = "documento_id,indice_chunk")
    })
public class DocumentoChunk extends BaseAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  @ToString.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "documento_id", nullable = false)
  private Documento documento;

  @Column(name = "indice_chunk", nullable = false)
  private Integer indiceChunk;

  @Lob
  @Column(name = "texto", nullable = false, columnDefinition = "LONGTEXT")
  private String texto;

  @Column(name = "pagina_origen")
  private Integer paginaOrigen;

  @Column(name = "token_count", nullable = false)
  private Integer tokenCount;

  @Lob
  @Column(name = "embedding", columnDefinition = "LONGTEXT")
  private String embedding;
}

