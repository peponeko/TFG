package com.easy4you.controller.api;

import com.easy4you.dto.contenido.ContenidoGeneradoResponseDTO;
import com.easy4you.dto.contenido.GenerarContenidoRequestDTO;
import com.easy4you.model.enums.TipoContenidoGenerado;
import com.easy4you.service.ContenidoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contenido")
@RequiredArgsConstructor

public class ContenidoController {

  private final ContenidoService contenidoService;

  @PostMapping("/generar")
  public ResponseEntity<ContenidoGeneradoResponseDTO> generar(@Valid @RequestBody GenerarContenidoRequestDTO request) {

    ContenidoGeneradoResponseDTO response = new ContenidoGeneradoResponseDTO();
    response.setTipo(request.getTipo());

    if (request.getTipo() == TipoContenidoGenerado.RESUMEN) {
      response.setResumen(contenidoService.generarResumen(request.getTexto()));
      return ResponseEntity.ok(response);
    }

    if (request.getTipo() == TipoContenidoGenerado.TEST) {
      response.setPreguntas(contenidoService.generarPreguntasTest(request.getTexto()));
      return ResponseEntity.ok(response);
    }

    response.setFlashcards(contenidoService.generarFlashcards(request.getTexto()));
    return ResponseEntity.ok(response);
  }
}

