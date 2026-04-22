package com.easy4you.controller.api;

import com.easy4you.dto.resumen.ResumenResponseDTO;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.Resumen;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.ResumenRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.ResumenGenerationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
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
  private final ResumenRepository resumenRepository;
  private final DocumentoRepository documentoRepository;
  private final TemaRepository temaRepository;
  private final ObjectMapper objectMapper;

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
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(resumen));
  }

  @GetMapping("/documento/{id}")
  public ResponseEntity<List<ResumenResponseDTO>> listarDocumento(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    Documento documento =
        documentoRepository
            .findByIdAndUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));

    List<ResumenResponseDTO> response =
        resumenRepository.findByDocumentoIdOrderByCreatedAtDesc(documento.getId()).stream()
            .filter(r -> r.getUsuario() != null && Objects.equals(r.getUsuario().getId(), usuarioActual.getId()))
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/tema/{id}")
  public ResponseEntity<List<ResumenResponseDTO>> listarTema(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    if (temaRepository.findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(id, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Tema no encontrado: " + id);
    }

    List<ResumenResponseDTO> response =
        resumenRepository.findByTemaIdOrderByCreatedAtDesc(id).stream()
            .filter(r -> r.getUsuario() != null && Objects.equals(r.getUsuario().getId(), usuarioActual.getId()))
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(response);
  }

  private ResumenResponseDTO toResponse(Resumen resumen) {
    List<String> puntosClave = parseStringList(resumen.getPuntosClaveJson());

    return new ResumenResponseDTO(
        resumen.getId(),
        resumen.getUsuario() != null ? resumen.getUsuario().getId() : null,
        resumen.getTema() != null ? resumen.getTema().getId() : null,
        resumen.getDocumento() != null ? resumen.getDocumento().getId() : null,
        resumen.getTitulo(),
        resumen.getContenido(),
        puntosClave,
        resumen.getOrigen(),
        resumen.getCreatedAt(),
        resumen.getUpdatedAt());
  }

  private List<String> parseStringList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
      if (list == null) {
        return List.of();
      }
      return list.stream().filter(Objects::nonNull).toList();
    } catch (Exception ex) {
      return List.of();
    }
  }
}
