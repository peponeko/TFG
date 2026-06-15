package com.easy4you.dto.evento;

import com.easy4you.model.enums.TipoEventoCalendario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoCalendarioRequestDTO {

  @NotBlank
  @Size(max = 200)
  private String titulo;

  private String descripcion;

  @NotNull
  private LocalDate fechaInicio;

  private LocalTime horaInicio;

  @NotNull
  private TipoEventoCalendario tipo;

  private Long asignaturaId;
}

