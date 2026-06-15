package com.easy4you.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "pregunta_test")
public class PreguntaTest extends BaseAuditableEntity {

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
  @Column(name = "enunciado", nullable = false)
  private String enunciado;

  @Lob
  @Column(name = "explicacion")
  private String explicacion;

  @Column(name = "dificultad", nullable = false)
  private Integer dificultad = 3;

  @OneToMany(mappedBy = "preguntaTest", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PreguntaTestOpcion> opciones = new ArrayList<>();
}
