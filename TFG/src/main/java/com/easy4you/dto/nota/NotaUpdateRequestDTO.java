package com.easy4you.dto.nota;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotaUpdateRequestDTO {

  @NotBlank
  @Size(max = 200)
  private String titulo;

  @NotBlank
  @Size(max = 20000)
  private String contenido;

  @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "colorHex debe tener formato #RRGGBB")
  private String colorHex;
}

