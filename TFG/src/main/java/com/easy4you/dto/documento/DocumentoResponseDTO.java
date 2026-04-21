package com.easy4you.dto.documento;

import com.easy4you.model.enums.EstadoProcesadoDocumento;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoResponseDTO {
  private Long id;
  private Long usuarioId;
  private Long asignaturaId;
  private Long temaId;

  private String nombreOriginal;
  private String rutaArchivo;
  private String mimeType;
  private String extension;
  private Long tamanoBytes;
  private String checksumSha256;
  private Integer paginas;

  private EstadoProcesadoDocumento estadoProcesado;
  private String errorExtraccion;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
