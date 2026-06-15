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
@Table(
    name = "chat_conversacion",
    indexes = {
      @Index(name = "idx_chat_conversacion_usuario", columnList = "usuario_id"),
      @Index(name = "idx_chat_conversacion_asignatura", columnList = "asignatura_id"),
      @Index(name = "idx_chat_conversacion_tema", columnList = "tema_id")
    })
public class ChatConversacion extends BaseAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  @ToString.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asignatura_id")
  private Asignatura asignatura;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tema_id")
  private Tema tema;

  @Column(name = "titulo", nullable = false, length = 200)
  private String titulo;

  @Lob
  @Column(name = "fuentes_activas", columnDefinition = "LONGTEXT")
  private String fuentesActivasJson;

  @OneToMany(mappedBy = "conversacion", fetch = FetchType.LAZY)
  private List<ChatMensaje> mensajes = new ArrayList<>();
}

