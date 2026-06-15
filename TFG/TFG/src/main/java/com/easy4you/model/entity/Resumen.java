package com.easy4you.model.entity;

import com.easy4you.model.enums.OrigenResumen;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "resumen")
public class Resumen extends BaseAuditableEntity {

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

  @Column(name = "titulo", nullable = false, length = 200)
  private String titulo;

  @Lob
  @Column(name = "contenido", nullable = false, columnDefinition = "LONGTEXT")
  private String contenido;

  @Lob
  @Column(name = "puntos_clave", columnDefinition = "LONGTEXT")
  private String puntosClaveJson;

  @Enumerated(EnumType.STRING)
  @Column(name = "origen", nullable = false, length = 10)
  private OrigenResumen origen = OrigenResumen.GENERADO;
}
