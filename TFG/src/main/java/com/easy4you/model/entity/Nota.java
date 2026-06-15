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
    name = "nota",
    indexes = {
      @Index(name = "idx_nota_usuario", columnList = "usuario_id"),
      @Index(name = "idx_nota_documento", columnList = "documento_id"),
      @Index(name = "idx_nota_chunk", columnList = "chunk_id"),
      @Index(name = "idx_nota_tema", columnList = "tema_id")
    })
public class Nota extends BaseAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  @ToString.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "documento_id")
  private Documento documento;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chunk_id")
  private DocumentoChunk chunk;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tema_id")
  private Tema tema;

  @Column(name = "titulo", nullable = false, length = 200)
  private String titulo;

  @Lob
  @Column(name = "contenido", nullable = false, columnDefinition = "LONGTEXT")
  private String contenido;

  @Column(name = "color_hex", length = 7)
  private String colorHex;
}

