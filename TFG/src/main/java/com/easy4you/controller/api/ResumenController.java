package com.easy4you.controller.api;

import com.easy4you.dto.resumen.ResumenResponseDTO;
import com.easy4you.mapper.ResumenMapper;
import com.easy4you.model.entity.Resumen;
import com.easy4you.model.entity.Usuario;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.ResumenGenerationService;
import com.easy4you.service.ResumenService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resumen")
@RequiredArgsConstructor
public class ResumenController {

  private final AuthenticatedUserService authenticatedUserService;
  private final ResumenGenerationService resumenGenerationService;
  private final ResumenService resumenService;

  @PostMapping("/generar/documento/{id}")
  public ResponseEntity<Map<String, String>> generarDocumento(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    resumenGenerationService.solicitarResumenDocumento(usuarioActual.getId(), id);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "processing"));
  }

  @PostMapping("/generar/tema/{id}")
  public ResponseEntity<ResumenResponseDTO> generarTema(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Resumen resumen = resumenGenerationService.generarResumenTema(usuarioActual.getId(), id);
    return ResponseEntity.status(HttpStatus.CREATED).body(ResumenMapper.toResponse(resumen));
  }

  @GetMapping("/documento/{id}")
  public ResponseEntity<List<ResumenResponseDTO>> listarDocumento(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    List<ResumenResponseDTO> response =
        resumenService.listarPorDocumento(usuarioActual.getId(), id).stream()
            .map(ResumenMapper::toResponse)
            .toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/tema/{id}")
  public ResponseEntity<List<ResumenResponseDTO>> listarTema(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    List<ResumenResponseDTO> response =
        resumenService.listarPorTema(usuarioActual.getId(), id).stream()
            .map(ResumenMapper::toResponse)
            .toList();
    return ResponseEntity.ok(response);
  }
}
