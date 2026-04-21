package com.easy4you.dto.documento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoRequestDTO {
  @NotNull
  private Long usuarioId;
  @NotNull
  private Long asignaturaId;
  private Long temaId; // opcional

  @NotBlank
  @Size(max = 255)
  private String nombreOriginal;

  @NotBlank
  @Size(max = 600)
  private String rutaArchivo;

  @NotBlank
  @Size(max = 100)
  private String mimeType;

  @NotBlank
  @Size(max = 10)
  private String extension;

  @NotNull
  private Long tamanoBytes;

  @Size(max = 64)
  private String checksumSha256;

  private Integer paginas;
}
