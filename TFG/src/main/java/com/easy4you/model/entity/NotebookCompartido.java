package com.easy4you.model.entity;

import com.easy4you.model.enums.RolNotebookCompartido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
    name = "notebook_compartido",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_notebook_compartido_asignatura_invitado",
          columnNames = {"asignatura_id", "usuario_invitado_id"})
    },
    indexes = {
      @Index(name = "idx_notebook_compartido_asignatura", columnList = "asignatura_id"),
      @Index(name = "idx_notebook_compartido_invitado", columnList = "usuario_invitado_id")
    })
public class NotebookCompartido extends BaseAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  @ToString.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asignatura_id", nullable = false)
  private Asignatura asignatura;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "propietario_id", nullable = false)
  private Usuario propietario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_invitado_id", nullable = false)
  private Usuario usuarioInvitado;

  @Enumerated(EnumType.STRING)
  @Column(name = "rol", nullable = false, length = 10)
  private RolNotebookCompartido rol = RolNotebookCompartido.VIEWER;
}

