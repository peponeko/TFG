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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tema")
public class Tema extends BaseAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  @ToString.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asignatura_id", nullable = false)
  private Asignatura asignatura;

  @Column(name = "trimestre")
  private Integer trimestre;

  @Column(name = "titulo", nullable = false, length = 200)
  @ToString.Include
  private String titulo;

  @Lob
  @Column(name = "descripcion")
  private String descripcion;

  @Column(name = "orden", nullable = false)
  private Integer orden = 0;

  @Column(name = "palabras_clave", length = 500)
  private String palabrasClave;

  @OneToMany(mappedBy = "tema", fetch = FetchType.LAZY)
  private Set<Documento> documentos = new HashSet<>();

  @OneToMany(mappedBy = "tema", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Resumen> resumenes = new HashSet<>();

  @OneToMany(mappedBy = "tema", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Flashcard> flashcards = new HashSet<>();

  @OneToMany(mappedBy = "tema", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<PreguntaTest> preguntasTest = new HashSet<>();

  @OneToMany(mappedBy = "tema", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ActividadRepaso> actividadesRepaso = new HashSet<>();

  @OneToMany(mappedBy = "tema", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ProgresoTema> progresos = new HashSet<>();

  @ManyToMany(mappedBy = "temas", fetch = FetchType.LAZY)
  private Set<SesionEstudio> sesionesEstudio = new HashSet<>();
}

