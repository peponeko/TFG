package com.easy4you.service.impl;

import com.easy4you.dto.tema.TemaPlanoResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.service.AsignaturaService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AsignaturaServiceImpl implements AsignaturaService {

  private final AsignaturaRepository asignaturaRepository;
  private final TemaRepository temaRepository;
  private final DocumentoRepository documentoRepository;
  private final FlashcardRepository flashcardRepository;
  private final PreguntaTestRepository preguntaTestRepository;

  @Override
  @Transactional(readOnly = true)
  public List<Asignatura> listar() {
    return asignaturaRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Asignatura> listarPorUsuarioId(Long usuarioId) {
    return asignaturaRepository.findByUsuarioIdOrderByNombreAsc(usuarioId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Asignatura> listarPorUsuarioIdYTrimestre(Long usuarioId, Integer trimestre) {
    if (trimestre == null) {
      return listarPorUsuarioId(usuarioId);
    }
    if (trimestre == 0) {
      return asignaturaRepository.findByUsuarioIdAndTrimestreIsNullOrderByNombreAsc(usuarioId);
    }
    if (trimestre == 1 || trimestre == 2 || trimestre == 3) {
      return asignaturaRepository.findByUsuarioIdAndTrimestreOrderByNombreAsc(usuarioId, trimestre);
    }
    throw new BadRequestException("trimestre inválido. Usa 1, 2, 3, 0 o omite el parámetro");
  }

  @Override
  @Transactional(readOnly = true)
  public Asignatura obtenerPorIdDeUsuario(Long usuarioId, Long asignaturaId) {
    return asignaturaRepository
        .findByIdAndUsuarioId(asignaturaId, usuarioId)
        .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + asignaturaId));
  }

  @Override
  public Asignatura crear(Asignatura asignatura) {
    return asignaturaRepository.save(asignatura);
  }

  @Override
  @Transactional(readOnly = true)
  public Asignatura obtenerPorId(Long id) {
    return asignaturaRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + id));
  }

  @Override
  public Asignatura actualizar(Long id, Asignatura datos) {
    Asignatura existente = obtenerPorId(id);
    existente.setNombre(datos.getNombre());
    existente.setDescripcion(datos.getDescripcion());
    existente.setColorHex(datos.getColorHex());
    existente.setTrimestre(datos.getTrimestre());
    return asignaturaRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!asignaturaRepository.existsById(id)) {
      throw new NotFoundException("Asignatura no encontrada: " + id);
    }
    asignaturaRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Long> resumenTrimestres(Long usuarioId, Long asignaturaId) {
    if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioId).isEmpty()) {
      throw new NotFoundException("Asignatura no encontrada: " + asignaturaId);
    }

    long trimestre1 = 0L;
    long trimestre2 = 0L;
    long trimestre3 = 0L;
    long sinAsignar = 0L;

    List<Object[]> rows = temaRepository.countTemasByAsignaturaIdGroupByTrimestre(asignaturaId);
    for (Object[] row : rows) {
      if (row == null || row.length < 2) {
        continue;
      }
      Integer tri = row[0] instanceof Number ? ((Number) row[0]).intValue() : null;
      long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;

      if (tri == null || tri == 0) {
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

    return Map.of(
        "trimestre1", trimestre1,
        "trimestre2", trimestre2,
        "trimestre3", trimestre3,
        "sinAsignar", sinAsignar);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TemaPlanoResponseDTO> temasPlanos(Long usuarioId, Long asignaturaId) {
    if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioId).isEmpty()) {
      throw new NotFoundException("Asignatura no encontrada: " + asignaturaId);
    }

    List<Object[]> temas = temaRepository.findTemasPlanosByAsignaturaId(asignaturaId);
    if (temas == null || temas.isEmpty()) {
      return List.of();
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
      Integer trimestre = row[4] instanceof Number ? ((Number) row[4]).intValue() : null;
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

    return response;
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
