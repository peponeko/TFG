package com.easy4you.model.entity;

import com.easy4you.model.enums.EstadoProcesadoDocumento;
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
@Table(name = "documento")
public class Documento extends BaseAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  @ToString.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asignatura_id", nullable = false)
  private Asignatura asignatura;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tema_id")
  private Tema tema;

  @Column(name = "nombre_original", nullable = false, length = 255)
  private String nombreOriginal;

  @Column(name = "ruta_archivo", nullable = false, length = 600)
  private String rutaArchivo;

  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;

  @Column(name = "extension", nullable = false, length = 10)
  private String extension;

  @Column(name = "tamano_bytes", nullable = false)
  private Long tamanoBytes;

  @Column(name = "checksum_sha256", length = 64)
  private String checksumSha256;

  @Lob
  @Column(name = "extraido_texto", columnDefinition = "LONGTEXT")
  private String textoExtraido;

  @Column(name = "paginas")
  private Integer paginas;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_procesado", nullable = false, length = 20)
  private EstadoProcesadoDocumento estadoProcesado = EstadoProcesadoDocumento.PENDIENTE;

  @Lob
  @Column(name = "error_procesado")
  private String errorExtraccion;

  @OneToMany(mappedBy = "documento", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<DocumentoChunk> chunks = new HashSet<>();

  @OneToMany(mappedBy = "documento", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Resumen> resumenes = new HashSet<>();

  @OneToMany(mappedBy = "documento", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Flashcard> flashcards = new HashSet<>();

  @OneToMany(mappedBy = "documento", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<PreguntaTest> preguntasTest = new HashSet<>();

  @OneToMany(mappedBy = "documento", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ActividadRepaso> actividadesRepaso = new HashSet<>();
}
