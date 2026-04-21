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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "asignatura")
public class Asignatura extends BaseAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  @ToString.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(name = "nombre", nullable = false, length = 120)
  @ToString.Include
  private String nombre;

  @Lob
  @Column(name = "descripcion")
  private String descripcion;

  @Column(name = "color_hex", length = 7)
  private String colorHex;

  @OneToMany(mappedBy = "asignatura", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ResultadoAprendizaje> resultadosAprendizaje = new HashSet<>();

  @OneToMany(mappedBy = "asignatura", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Documento> documentos = new HashSet<>();

  @OneToMany(mappedBy = "asignatura", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ProgresoAsignatura> progresos = new HashSet<>();
}

