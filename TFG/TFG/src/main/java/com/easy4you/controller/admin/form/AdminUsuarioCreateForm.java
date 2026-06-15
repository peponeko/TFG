package com.easy4you.controller.admin.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUsuarioCreateForm {

  @NotBlank(message = "El nombre es obligatorio")
  @Size(max = 100, message = "El nombre es demasiado largo")
  private String nombre;

  @Size(max = 150, message = "Los apellidos son demasiado largos")
  private String apellidos;

  @NotBlank(message = "El email es obligatorio")
  @Email(message = "El email no es válido")
  @Size(max = 190, message = "El email es demasiado largo")
  private String email;

  @NotBlank(message = "La contraseña es obligatoria")
  @Size(min = 8, max = 72, message = "La contraseña debe tener al menos 8 caracteres")
  private String password;

  private Boolean activo = true;
  private Boolean verificado = false;
}

