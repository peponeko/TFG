package com.easy4you.controller.admin.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminAsignaturaCreateForm {

  @NotNull(message = "Selecciona un usuario")
  private Long usuarioId;

  @NotBlank(message = "El nombre es obligatorio")
  @Size(max = 120, message = "El nombre es demasiado largo")
  private String nombre;

  @Size(max = 2000, message = "La descripción es demasiado larga")
  private String descripcion;

  @Size(max = 7, message = "Color no válido")
  private String colorHex;

  @Min(value = 0, message = "Trimestre no válido")
  @Max(value = 3, message = "Trimestre no válido")
  private Integer trimestre;
}

