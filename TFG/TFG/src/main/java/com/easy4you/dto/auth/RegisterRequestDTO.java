package com.easy4you.dto.auth;

import jakarta.validation.constraints.Email;
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
public class RegisterRequestDTO {
  @NotBlank
  @Size(max = 100)
  private String nombre;

  @Size(max = 150)
  private String apellidos;

  @NotBlank
  @Email
  @Size(max = 190)
  private String email;

  @NotBlank
  @Size(min = 6, max = 100)
  private String password;
}

