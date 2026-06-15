package com.easy4you.model.entity;

import com.easy4you.model.enums.EstadoSesionEstudio;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
@Table(name = "sesion_estudio")
public class SesionEstudio extends BaseAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  @ToString.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(name = "titulo", nullable = false, length = 200)
  private String titulo;

  @Lob
  @Column(name = "descripcion")
  private String descripcion;

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDateTime fechaInicio;

  @Column(name = "fecha_fin", nullable = false)
  private LocalDateTime fechaFin;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 15)
  private EstadoSesionEstudio estado = EstadoSesionEstudio.PLANIFICADA;

  @Column(name = "minutos_objetivo")
  private Integer minutosObjetivo;

  @Column(name = "minutos_real")
  private Integer minutosReal;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "sesion_estudio_tema",
      joinColumns = @JoinColumn(name = "sesion_estudio_id"),
      inverseJoinColumns = @JoinColumn(name = "tema_id"))
  private Set<Tema> temas = new HashSet<>();

  @OneToMany(mappedBy = "sesionEstudio", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Recordatorio> recordatorios = new HashSet<>();
}

