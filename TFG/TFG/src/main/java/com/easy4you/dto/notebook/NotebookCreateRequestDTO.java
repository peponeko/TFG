package com.easy4you.dto.notebook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotebookCreateRequestDTO {

  @NotBlank
  @Size(max = 120)
  private String nombre;

  @Size(max = 5000)
  private String descripcion;

  @Size(max = 7)
  private String colorHex;
}

