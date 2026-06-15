package com.easy4you.model.entity;

import com.easy4you.model.enums.ChatRol;
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
    name = "chat_mensaje",
    indexes = {@Index(name = "idx_chat_mensaje_conversacion", columnList = "conversacion_id")})
public class ChatMensaje extends BaseAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  @ToString.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversacion_id", nullable = false)
  private ChatConversacion conversacion;

  @Enumerated(EnumType.STRING)
  @Column(name = "rol", nullable = false, length = 20)
  private ChatRol rol;

  @Lob
  @Column(name = "contenido", nullable = false, columnDefinition = "LONGTEXT")
  private String contenido;

  @Lob
  @Column(name = "fuentes_usadas", columnDefinition = "LONGTEXT")
  private String fuentesUsadasJson;
}

