package com.easy4you.dto.notebook;

import com.easy4you.model.enums.RolNotebookCompartido;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotebookCompartidoResponseDTO {
  private Long id;
  private Long asignaturaId;
  private Long propietarioId;
  private Long usuarioInvitadoId;
  private RolNotebookCompartido rol;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

