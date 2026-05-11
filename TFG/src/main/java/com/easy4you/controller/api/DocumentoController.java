package com.easy4you.controller.api;

import com.easy4you.dto.documento.DocumentoBusquedaResultadoDTO;
import com.easy4you.dto.documento.DocumentoDetalleResponseDTO;
import com.easy4you.dto.documento.DocumentoEstadoResponseDTO;
import com.easy4you.dto.documento.DocumentoRequestDTO;
import com.easy4you.dto.documento.DocumentoChunksPageResponseDTO;
import com.easy4you.dto.documento.DocumentoResponseDTO;
import com.easy4you.dto.documento.DocumentoUploadResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.mapper.DocumentoMapper;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Documento;
import com.easy4you.model.entity.Tema;
import com.easy4you.model.entity.Usuario;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.AsignaturaService;
import com.easy4you.service.DocumentoIngestionService;
import com.easy4you.service.DocumentoProcessingService;
import com.easy4you.service.DocumentoService;
import com.easy4you.service.DocumentoUploadResult;
import com.easy4you.service.TemaService;
import com.easy4you.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

  private final DocumentoService documentoService;
  private final UsuarioService usuarioService;
  private final AsignaturaService asignaturaService;
  private final TemaService temaService;
  private final DocumentoIngestionService documentoIngestionService;
  private final DocumentoProcessingService documentoProcessingService;
  private final AuthenticatedUserService authenticatedUserService;

  @GetMapping
  public ResponseEntity<List<DocumentoResponseDTO>> listar(
      @RequestParam(required = false) Long usuarioId,
      @RequestParam(required = false) Long temaId,
      @RequestParam(required = false) Long asignaturaId) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    List<Documento> documentos;
    if (temaId != null) {
      documentos =
          documentoService.listarPorTemaId(temaId).stream()
              .filter(d -> d.getUsuario() != null && usuarioActual.getId().equals(d.getUsuario().getId()))
              .toList();
    } else if (asignaturaId != null) {
      documentos = documentoService.listarPorAsignaturaIdDeUsuario(usuarioActual.getId(), asignaturaId);
    } else {
      documentos = documentoService.listarPorUsuarioId(usuarioActual.getId());
    }

    List<DocumentoResponseDTO> response = documentos.stream().map(DocumentoMapper::toResponse).toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<DocumentoResponseDTO> obtener(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Documento documento = documentoService.obtenerPorIdDeUsuario(usuarioActual.getId(), id);
    return ResponseEntity.ok(DocumentoMapper.toResponse(documento));
  }

  @GetMapping("/{id}/estado")
  public ResponseEntity<DocumentoEstadoResponseDTO> estado(@PathVariable Long id) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    return ResponseEntity.ok(documentoService.estado(usuarioId, id));
  }

  @PostMapping("/upload")
  public ResponseEntity<DocumentoUploadResponseDTO> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("asignaturaId") Long asignaturaId,
      @RequestParam(value = "temaId", required = false) Long temaId) {

    DocumentoUploadResult result = documentoIngestionService.upload(file, asignaturaId, temaId);
    List<DocumentoResponseDTO> documentos = result.documentos().stream().map(DocumentoMapper::toResponse).toList();

    HttpStatus status = HttpStatus.OK;
    if (result.warnings() == null || result.warnings().isEmpty()) {
      status = HttpStatus.CREATED;
    }

    return ResponseEntity.status(status).body(new DocumentoUploadResponseDTO(documentos, result.warnings()));
  }

  @GetMapping("/{id}/chunks")
  public ResponseEntity<DocumentoChunksPageResponseDTO> chunks(
      @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    return ResponseEntity.ok(documentoService.chunks(usuarioId, id, pageable));
  }

  @GetMapping("/buscar")
  public ResponseEntity<List<DocumentoBusquedaResultadoDTO>> buscar(
      @RequestParam("q") String q,
      @RequestParam(required = false) Long asignaturaId,
      @RequestParam(required = false) Long temaId) {

    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    return ResponseEntity.ok(documentoService.buscar(usuarioId, q, asignaturaId, temaId));
  }

  @GetMapping("/{id}/detalle")
  public ResponseEntity<DocumentoDetalleResponseDTO> detalle(
      @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    return ResponseEntity.ok(documentoService.detalle(usuarioId, id, pageable));
  }

  @PostMapping
  public ResponseEntity<DocumentoResponseDTO> crear(@Valid @RequestBody DocumentoRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    if (!usuarioActual.getId().equals(request.getUsuarioId())) {
      throw new BadRequestException("usuarioId no coincide con el usuario autenticado");
    }

    Usuario usuario = usuarioService.obtenerPorId(request.getUsuarioId());
    Asignatura asignatura = asignaturaService.obtenerPorId(request.getAsignaturaId());

    Tema tema = null;
    if (request.getTemaId() != null) {
      tema = temaService.obtenerPorId(request.getTemaId());
    }

    Documento documento = new Documento();
    documento.setUsuario(usuario);
    documento.setAsignatura(asignatura);
    documento.setTema(tema);
    documento.setNombreOriginal(request.getNombreOriginal());
    documento.setRutaArchivo(request.getRutaArchivo());
    documento.setMimeType(request.getMimeType());
    documento.setExtension(request.getExtension());
    documento.setTamanoBytes(request.getTamanoBytes());
    documento.setChecksumSha256(request.getChecksumSha256());
    documento.setPaginas(request.getPaginas());

    Documento creado = documentoService.crear(documento);
    return ResponseEntity.status(HttpStatus.CREATED).body(DocumentoMapper.toResponse(creado));
  }

  @PutMapping("/{id}")
  public ResponseEntity<DocumentoResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody DocumentoRequestDTO request) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Documento existente = documentoService.obtenerPorIdDeUsuario(usuarioActual.getId(), id);

    Documento datos = new Documento();
    datos.setNombreOriginal(request.getNombreOriginal());
    datos.setRutaArchivo(request.getRutaArchivo());
    datos.setMimeType(request.getMimeType());
    datos.setExtension(request.getExtension());
    datos.setTamanoBytes(request.getTamanoBytes());
    datos.setChecksumSha256(request.getChecksumSha256());
    datos.setPaginas(request.getPaginas());
    datos.setTextoExtraido(existente.getTextoExtraido());
    datos.setEstadoProcesado(existente.getEstadoProcesado());
    datos.setErrorExtraccion(existente.getErrorExtraccion());

    if (request.getTemaId() != null) {
      datos.setTema(temaService.obtenerPorId(request.getTemaId()));
    } else {
      datos.setTema(existente.getTema());
    }

    Documento actualizado = documentoService.actualizar(id, datos);
    return ResponseEntity.ok(DocumentoMapper.toResponse(actualizado));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    documentoService.obtenerPorIdDeUsuario(usuarioId, id);
    documentoService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/reprocesar")
  public ResponseEntity<Map<String, String>> reprocesar(@PathVariable Long id) {
    Long usuarioId = authenticatedUserService.requireUsuarioActual().getId();
    Documento documento = documentoService.obtenerPorIdDeUsuario(usuarioId, id);
    documentoProcessingService.procesarAsync(documento.getId());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "processing"));
  }

}
