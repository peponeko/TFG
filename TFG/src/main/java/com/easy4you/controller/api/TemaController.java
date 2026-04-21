package com.easy4you.controller.api;

import com.easy4you.dto.tema.TemaRequestDTO;
import com.easy4you.dto.tema.TemaResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.ResultadoAprendizaje;
import com.easy4you.model.entity.Tema;
import com.easy4you.model.entity.Unidad;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.ResultadoAprendizajeRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.repository.UnidadRepository;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.TemaService;
import com.easy4you.service.UnidadService;
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
@RequestMapping("/api/temas")
@RequiredArgsConstructor
public class TemaController {

  private final TemaService temaService;
  private final UnidadService unidadService;
  private final AuthenticatedUserService authenticatedUserService;
  private final AsignaturaRepository asignaturaRepository;
  private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
  private final UnidadRepository unidadRepository;
  private final TemaRepository temaRepository;

  @GetMapping
  public ResponseEntity<List<TemaResponseDTO>> listar(
      @RequestParam(required = false) Long unidadId, @RequestParam(required = false) Long asignaturaId) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    List<Tema> temas;
    if (asignaturaId != null) {
      if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioActual.getId()).isEmpty()) {
        throw new NotFoundException("Asignatura no encontrada: " + asignaturaId);
      }
      temas = temaRepository.findByUnidadResultadoAprendizajeAsignaturaIdOrderByOrdenAsc(asignaturaId);
    } else if (unidadId != null) {
      if (!unidadRepository.existsByIdAndResultadoAprendizajeAsignaturaUsuarioId(unidadId, usuarioActual.getId())) {
        throw new NotFoundException("Unidad no encontrada: " + unidadId);
      }
      temas = temaService.listarPorUnidadId(unidadId);
    } else {
      throw new BadRequestException("unidadId o asignaturaId es obligatorio");
    }

    List<TemaResponseDTO> response = temas.stream().map(this::toResponse).toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TemaResponseDTO> obtener(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Tema tema =
        temaRepository
            .findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Tema no encontrado: " + id));

    return ResponseEntity.ok(toResponse(tema));
  }

  @PostMapping
  public ResponseEntity<TemaResponseDTO> crear(@Valid @RequestBody TemaRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    Unidad unidad = resolveUnidadParaCrearTema(usuarioActual.getId(), request);

    Tema tema = new Tema();
    tema.setUnidad(unidad);
    tema.setTitulo(request.getTitulo());
    tema.setDescripcion(request.getDescripcion());
    tema.setOrden(request.getOrden() != null ? request.getOrden() : 0);
    tema.setPalabrasClave(request.getPalabrasClave());

    Tema creado = temaService.crear(tema);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(creado));
  }

  @PutMapping("/{id}")
  public ResponseEntity<TemaResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody TemaRequestDTO request) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    Tema existente =
        temaRepository
            .findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Tema no encontrado: " + id));

    Tema datos = new Tema();
    datos.setUnidad(existente.getUnidad());
    datos.setTitulo(request.getTitulo());
    datos.setDescripcion(request.getDescripcion());
    datos.setOrden(request.getOrden() != null ? request.getOrden() : existente.getOrden());
    datos.setPalabrasClave(request.getPalabrasClave());

    if (request.getUnidadId() != null
        && (existente.getUnidad() == null || !request.getUnidadId().equals(existente.getUnidad().getId()))) {
      if (!unidadRepository.existsByIdAndResultadoAprendizajeAsignaturaUsuarioId(request.getUnidadId(), usuarioActual.getId())) {
        throw new NotFoundException("Unidad no encontrada: " + request.getUnidadId());
      }
      datos.setUnidad(unidadService.obtenerPorId(request.getUnidadId()));
    }

    Tema actualizado = temaService.actualizar(id, datos);
    return ResponseEntity.ok(toResponse(actualizado));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    if (temaRepository.findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(id, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Tema no encontrado: " + id);
    }

    temaService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  private Unidad resolveUnidadParaCrearTema(Long usuarioId, TemaRequestDTO request) {
    if (request == null) {
      throw new BadRequestException("Body es obligatorio");
    }

    if (request.getUnidadId() != null) {
      if (!unidadRepository.existsByIdAndResultadoAprendizajeAsignaturaUsuarioId(request.getUnidadId(), usuarioId)) {
        throw new NotFoundException("Unidad no encontrada: " + request.getUnidadId());
      }
      return unidadService.obtenerPorId(request.getUnidadId());
    }

    if (request.getAsignaturaId() == null) {
      throw new BadRequestException("unidadId o asignaturaId es obligatorio");
    }

    Asignatura asignatura =
        asignaturaRepository
            .findByIdAndUsuarioId(request.getAsignaturaId(), usuarioId)
            .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + request.getAsignaturaId()));

    ResultadoAprendizaje ra = resolveResultadoAprendizajePorDefecto(asignatura);
    Unidad unidad = resolveUnidadPorDefecto(ra);
    return unidad;
  }

  private ResultadoAprendizaje resolveResultadoAprendizajePorDefecto(Asignatura asignatura) {
    List<ResultadoAprendizaje> ras =
        resultadoAprendizajeRepository.findByAsignaturaIdOrderByOrdenAsc(asignatura.getId());
    if (ras != null && !ras.isEmpty()) {
      return ras.get(0);
    }

    ResultadoAprendizaje nuevo = new ResultadoAprendizaje();
    nuevo.setAsignatura(asignatura);
    nuevo.setCodigo("GEN");
    nuevo.setDescripcion("Resultado de aprendizaje general (creado automáticamente)");
    nuevo.setOrden(0);
    return resultadoAprendizajeRepository.save(nuevo);
  }

  private Unidad resolveUnidadPorDefecto(ResultadoAprendizaje ra) {
    List<Unidad> unidades = unidadRepository.findByResultadoAprendizajeIdOrderByOrdenAsc(ra.getId());
    if (unidades != null && !unidades.isEmpty()) {
      return unidades.get(0);
    }

    Unidad unidad = new Unidad();
    unidad.setResultadoAprendizaje(ra);
    unidad.setTitulo("General");
    unidad.setDescripcion("Unidad por defecto (creada automáticamente)");
    unidad.setOrden(0);
    return unidadRepository.save(unidad);
  }

  private TemaResponseDTO toResponse(Tema tema) {
    return new TemaResponseDTO(
        tema.getId(),
        tema.getUnidad() != null ? tema.getUnidad().getId() : null,
        tema.getTitulo(),
        tema.getDescripcion(),
        tema.getOrden(),
        tema.getPalabrasClave(),
        tema.getCreatedAt(),
        tema.getUpdatedAt());
  }
}
