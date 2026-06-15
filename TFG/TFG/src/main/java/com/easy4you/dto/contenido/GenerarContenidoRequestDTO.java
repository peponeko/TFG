package com.easy4you.dto.contenido;

import com.easy4you.model.enums.TipoContenidoGenerado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerarContenidoRequestDTO {
  @NotBlank
  private String texto;

  @NotNull
  private TipoContenidoGenerado tipo;
}

