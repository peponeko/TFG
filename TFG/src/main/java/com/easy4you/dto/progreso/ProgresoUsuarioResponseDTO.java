package com.easy4you.dto.progreso;

import com.easy4you.dto.sesion.SesionEstudioResponseDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgresoUsuarioResponseDTO {
  private List<SesionEstudioResponseDTO> sesionesRecientes;
  private long flashcardsRepasadas;
  private long testsCompletados;
  private List<ProgresoTemaResponseDTO> dominioPorTema;
}

