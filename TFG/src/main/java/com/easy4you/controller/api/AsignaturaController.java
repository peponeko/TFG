package com.easy4you.controller.api;

import com.easy4you.dto.asignatura.AsignaturaRequestDTO;
import com.easy4you.dto.asignatura.AsignaturaResponseDTO;
import com.easy4you.dto.tema.TemaPlanoResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Usuario;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.security.AuthenticatedUserService;
import com.easy4you.service.AsignaturaService;
import com.easy4you.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/asignaturas")
@RequiredArgsConstructor
public class AsignaturaController {

  private final AsignaturaService asignaturaService;
  private final UsuarioService usuarioService;
  private final AuthenticatedUserService authenticatedUserService;
  private final AsignaturaRepository asignaturaRepository;
  private final TemaRepository temaRepository;
  private final DocumentoRepository documentoRepository;
  private final FlashcardRepository flashcardRepository;
  private final PreguntaTestRepository preguntaTestRepository;

  @GetMapping
  public ResponseEntity<List<AsignaturaResponseDTO>> listar(
      @RequestParam(required = false) Long usuarioId, @RequestParam(required = false) Integer trimestre) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    if (usuarioId != null && !usuarioId.equals(usuarioActual.getId())) {
      throw new BadRequestException("usuarioId no coincide con el usuario autenticado");
    }

    List<Asignatura> asignaturas;
    if (trimestre == null) {
      asignaturas = new ArrayList<>(asignaturaService.listarPorUsuarioId(usuarioActual.getId()));
      asignaturas.sort(
          Comparator.comparing(
                  Asignatura::getTrimestre, Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(a -> a.getNombre() == null ? "" : a.getNombre(), String.CASE_INSENSITIVE_ORDER));
    } else if (trimestre == 0) {
      asignaturas =
          asignaturaRepository.findByUsuarioIdAndTrimestreIsNullOrderByNombreAsc(usuarioActual.getId());
    } else if (trimestre == 1 || trimestre == 2 || trimestre == 3) {
      asignaturas =
          asignaturaRepository.findByUsuarioIdAndTrimestreOrderByNombreAsc(usuarioActual.getId(), trimestre);
    } else {
      throw new BadRequestException("trimestre inválido. Usa 1, 2, 3, 0 o omite el parámetro");
    }

    List<AsignaturaResponseDTO> response = asignaturas.stream().map(this::toResponse).toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<AsignaturaResponseDTO> obtener(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Asignatura asignatura =
        asignaturaRepository
            .findByIdAndUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + id));

    return ResponseEntity.ok(toResponse(asignatura));
  }

  @PostMapping
  public ResponseEntity<AsignaturaResponseDTO> crear(@Valid @RequestBody AsignaturaRequestDTO request) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    if (request.getUsuarioId() != null && !request.getUsuarioId().equals(usuarioActual.getId())) {
      throw new BadRequestException("usuarioId no coincide con el usuario autenticado");
    }

    Usuario usuario = usuarioService.obtenerPorId(usuarioActual.getId());

    Asignatura asignatura = new Asignatura();
    asignatura.setUsuario(usuario);
    asignatura.setNombre(request.getNombre());
    asignatura.setDescripcion(request.getDescripcion());
    asignatura.setColorHex(request.getColorHex());
    asignatura.setTrimestre(request.getTrimestre());

    Asignatura creada = asignaturaService.crear(asignatura);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(creada));
  }

  @PutMapping("/{id}")
  public ResponseEntity<AsignaturaResponseDTO> actualizar(
      @PathVariable Long id, @Valid @RequestBody AsignaturaRequestDTO request) {

    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    Asignatura existente =
        asignaturaRepository
            .findByIdAndUsuarioId(id, usuarioActual.getId())
            .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + id));

    Asignatura datos = new Asignatura();
    datos.setNombre(request.getNombre());
    datos.setDescripcion(request.getDescripcion());
    datos.setColorHex(request.getColorHex());
    datos.setTrimestre(request.getTrimestre());
    datos.setUsuario(existente.getUsuario());

    Asignatura actualizada = asignaturaService.actualizar(id, datos);
    return ResponseEntity.ok(toResponse(actualizada));
  }

  @GetMapping("/{id}/resumen-trimestres")
  public ResponseEntity<Map<String, Long>> resumenTrimestres(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    if (asignaturaRepository.findByIdAndUsuarioId(id, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Asignatura no encontrada: " + id);
    }

    long trimestre1 = 0L;
    long trimestre2 = 0L;
    long trimestre3 = 0L;
    long sinAsignar = 0L;

    List<Object[]> rows = temaRepository.countTemasByAsignaturaIdGroupByResultadoCodigo(id);
    for (Object[] row : rows) {
      if (row == null || row.length < 2) {
        continue;
      }

      String codigo = row[0] == null ? null : String.valueOf(row[0]);
      long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;

      Integer tri = parseTrimestreFromCodigo(codigo);
      if (tri == null) {
        sinAsignar += count;
      } else if (tri == 1) {
        trimestre1 += count;
      } else if (tri == 2) {
        trimestre2 += count;
      } else if (tri == 3) {
        trimestre3 += count;
      } else {
        sinAsignar += count;
      }
    }

    return ResponseEntity.ok(
        Map.of(
            "trimestre1",
            trimestre1,
            "trimestre2",
            trimestre2,
            "trimestre3",
            trimestre3,
            "sinAsignar",
            sinAsignar));
  }

  @GetMapping("/{id}/temas-planos")
  public ResponseEntity<List<TemaPlanoResponseDTO>> temasPlanos(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();

    if (asignaturaRepository.findByIdAndUsuarioId(id, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Asignatura no encontrada: " + id);
    }

    List<Object[]> temas = temaRepository.findTemasPlanosByAsignaturaId(id);
    if (temas == null || temas.isEmpty()) {
      return ResponseEntity.ok(List.of());
    }

    List<Long> temaIds =
        temas.stream()
            .map(r -> r != null && r.length > 0 && r[0] instanceof Number ? ((Number) r[0]).longValue() : null)
            .filter(v -> v != null && v > 0)
            .distinct()
            .toList();

    Map<Long, Long> docsCount =
        temaIds.isEmpty() ? Map.of() : toCountMap(documentoRepository.countDocumentosByTemaIds(temaIds));
    Map<Long, Long> flashCount =
        temaIds.isEmpty() ? Map.of() : toCountMap(flashcardRepository.countFlashcardsByTemaIds(temaIds));
    Map<Long, Long> pregCount =
        temaIds.isEmpty() ? Map.of() : toCountMap(preguntaTestRepository.countPreguntasByTemaIds(temaIds));

    List<TemaPlanoResponseDTO> response = new ArrayList<>();
    for (Object[] row : temas) {
      if (row == null || row.length < 5) {
        continue;
      }
      Long temaId = row[0] instanceof Number ? ((Number) row[0]).longValue() : null;
      if (temaId == null) {
        continue;
      }
      String titulo = row[1] == null ? null : String.valueOf(row[1]);
      String descripcion = row[2] == null ? null : String.valueOf(row[2]);
      String palabrasClave = row[3] == null ? null : String.valueOf(row[3]);
      String codigo = row[4] == null ? null : String.valueOf(row[4]);

      Integer trimestre = parseTrimestreFromCodigo(codigo);
      response.add(
          new TemaPlanoResponseDTO(
              temaId,
              titulo,
              descripcion,
              palabrasClave,
              trimestre,
              docsCount.getOrDefault(temaId, 0L),
              flashCount.getOrDefault(temaId, 0L),
              pregCount.getOrDefault(temaId, 0L)));
    }

    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    Usuario usuarioActual = authenticatedUserService.requireUsuarioActual();
    if (asignaturaRepository.findByIdAndUsuarioId(id, usuarioActual.getId()).isEmpty()) {
      throw new NotFoundException("Asignatura no encontrada: " + id);
    }

    asignaturaService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  private AsignaturaResponseDTO toResponse(Asignatura asignatura) {
    return new AsignaturaResponseDTO(
        asignatura.getId(),
        asignatura.getUsuario() != null ? asignatura.getUsuario().getId() : null,
        asignatura.getNombre(),
        asignatura.getDescripcion(),
        asignatura.getColorHex(),
        asignatura.getTrimestre(),
        asignatura.getCreatedAt(),
        asignatura.getUpdatedAt());
  }

  private Integer parseTrimestreFromCodigo(String codigo) {
    if (codigo == null) {
      return null;
    }
    String c = codigo.trim().toUpperCase();
    if (c.length() == 2 && c.charAt(0) == 'T') {
      char n = c.charAt(1);
      if (n == '1') return 1;
      if (n == '2') return 2;
      if (n == '3') return 3;
    }
    return null;
  }

  private Map<Long, Long> toCountMap(List<Object[]> rows) {
    Map<Long, Long> map = new HashMap<>();
    if (rows == null) {
      return map;
    }
    for (Object[] row : rows) {
      if (row == null || row.length < 2) {
        continue;
      }
      Long id = row[0] instanceof Number ? ((Number) row[0]).longValue() : null;
      if (id == null) {
        continue;
      }
      long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
      map.put(id, count);
    }
    return map;
  }
}
