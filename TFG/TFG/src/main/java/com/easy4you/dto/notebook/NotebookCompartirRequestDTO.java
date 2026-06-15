package com.easy4you.dto.notebook;

import com.easy4you.model.enums.RolNotebookCompartido;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotebookCompartirRequestDTO {

  @NotNull
  private Long asignaturaId;

  @NotNull
  private Long usuarioInvitadoId;

  private RolNotebookCompartido rol;
}

