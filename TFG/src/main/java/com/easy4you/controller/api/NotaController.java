package com.easy4you.controller.api;

import com.easy4you.dto.nota.NotaCreateRequestDTO;
import com.easy4you.dto.nota.NotaResponseDTO;
import com.easy4you.dto.nota.NotaUpdateRequestDTO;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.Nota;
import com.easy4you.model.entity.Tema;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.NotaService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notas")
@RequiredArgsConstructor
public class NotaController {

  private final AuthenticatedUserService authenticatedUserService;
  private final NotaService notaService;
  private final DocumentoRepository documentoRepository;
  private final TemaRepository temaRepository;
  private final AsignaturaRepository asignaturaRepository;

  @PostMapping
  public ResponseEntity<NotaResponseDTO> crear(@Valid @RequestBody NotaCreateRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    // En el esquema simplificado no usamos chunks. Se ignora chunkId si llega por compatibilidad.

    Documento documento = null;
    if (request.getDocumentoId() != null) {
      documento =
          documentoRepository
              .findByIdAndUsuarioId(request.getDocumentoId(), usuarioActual.getId())
              .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + request.getDocumentoId()));
    }

    Tema tema = null;
    if (request.getTemaId() != null) {
      tema =
          temaRepository
              .findByIdAndAsignaturaUsuarioId(request.getTemaId(), usuarioActual.getId())
              .orElseThrow(() -> new NotFoundException("Tema no encontrado: " + request.getTemaId()));
    } else if (documento != null) {
      tema = documento.getTema();
    }

    Nota nota = new Nota();
    nota.setUsuario(usuarioActual);
    nota.setDocumento(documento);
    nota.setChunk(null);
    nota.setTema(tema);
    nota.setTitulo(request.getTitulo().trim());
    nota.setContenido(request.getContenido().trim());
    nota.setColorHex(request.getColorHex());

    Nota saved = notaService.crear(nota);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
  }

  @GetMapping
  public ResponseEntity<List<NotaResponseDTO>> listar(
      @RequestParam(required = false) Long documentoId,
      @RequestParam(required = false) Long temaId,
      @RequestParam(required = false) Long asignaturaId) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    if (documentoId != null
        && documentoRepository.findByIdAndUsuarioId(documentoId, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Documento no encontrado: " + documentoId);
    }
    if (temaId != null
        && temaRepository.findByIdAndAsignaturaUsuarioId(temaId, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Tema no encontrado: " + temaId);
    }
    if (asignaturaId != null && asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Asignatura no encontrada: " + asignaturaId);
    }

    List<NotaResponseDTO> response =
        notaService.listar(usuarioActual.getId(), documentoId, temaId, asignaturaId).stream()
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<NotaResponseDTO> obtener(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Nota nota = notaService.obtenerPorId(usuarioActual.getId(), id);
    return ResponseEntity.ok(toResponse(nota));
  }

  @PutMapping("/{id}")
  public ResponseEntity<NotaResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody NotaUpdateRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    Nota datos = new Nota();
    datos.setTitulo(request.getTitulo().trim());
    datos.setContenido(request.getContenido().trim());
    datos.setColorHex(request.getColorHex());

    Nota updated = notaService.actualizar(usuarioActual.getId(), id, datos);
    return ResponseEntity.ok(toResponse(updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    notaService.eliminar(usuarioActual.getId(), id);
    return ResponseEntity.noContent().build();
  }

  private NotaResponseDTO toResponse(Nota nota) {
    return new NotaResponseDTO(
        nota.getId(),
        nota.getUsuario() != null ? nota.getUsuario().getId() : null,
        nota.getDocumento() != null ? nota.getDocumento().getId() : null,
        nota.getChunk() != null ? nota.getChunk().getId() : null,
        nota.getTema() != null ? nota.getTema().getId() : null,
        nota.getTitulo(),
        nota.getContenido(),
        nota.getColorHex(),
        nota.getCreatedAt(),
        nota.getUpdatedAt());
  }
}

