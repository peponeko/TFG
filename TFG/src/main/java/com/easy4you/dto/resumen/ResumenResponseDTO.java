package com.easy4you.dto.resumen;

import com.easy4you.model.enums.OrigenResumen;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumenResponseDTO {
  private Long id;
  private Long usuarioId;
  private Long temaId;
  private Long documentoId;
  private String titulo;
  private String contenido;
  private List<String> puntosClave;
  private OrigenResumen origen;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

