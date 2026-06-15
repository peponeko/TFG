package com.easy4you.dto.evento;

import com.easy4you.model.enums.TipoEventoCalendario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoCalendarioResponseDTO {
  private Long id;
  private Long usuarioId;
  private Long asignaturaId;
  private String titulo;
  private String descripcion;
  private LocalDate fechaInicio;
  private LocalTime horaInicio;
  private TipoEventoCalendario tipo;
  private boolean completado;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

