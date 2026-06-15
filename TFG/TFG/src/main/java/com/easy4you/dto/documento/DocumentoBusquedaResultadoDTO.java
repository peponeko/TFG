package com.easy4you.dto.documento;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoBusquedaResultadoDTO {
  private Long documentoId;
  private String documentoNombre;
  private List<DocumentoBusquedaFragmentDTO> fragmentos;
}

