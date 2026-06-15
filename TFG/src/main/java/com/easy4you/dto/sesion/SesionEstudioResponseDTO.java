package com.easy4you.dto.sesion;

import com.easy4you.model.enums.EstadoSesionEstudio;
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
public class SesionEstudioResponseDTO {
  private Long id;
  private Long usuarioId;
  private String titulo;
  private String descripcion;
  private LocalDateTime fechaInicio;
  private LocalDateTime fechaFin;
  private EstadoSesionEstudio estado;
  private Integer minutosObjetivo;
  private Integer minutosReal;
  private List<Long> temaIds;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

