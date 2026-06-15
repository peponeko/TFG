package com.easy4you.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "flashcard")
public class Flashcard extends BaseAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  @ToString.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tema_id", nullable = false)
  private Tema tema;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "documento_id")
  private Documento documento;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chunk_origen_id")
  private DocumentoChunk chunkOrigen;

  @Lob
  @Column(name = "pregunta", nullable = false)
  private String pregunta;

  @Lob
  @Column(name = "respuesta", nullable = false)
  private String respuesta;

  @Column(name = "dificultad", nullable = false)
  private Integer dificultad = 3;
}
