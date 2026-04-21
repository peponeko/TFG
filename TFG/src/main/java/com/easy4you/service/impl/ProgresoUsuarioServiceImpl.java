package com.easy4you.service.impl;

import com.easy4you.dto.progreso.ProgresoTemaResponseDTO;
import com.easy4you.dto.progreso.ProgresoUsuarioResponseDTO;
import com.easy4you.dto.sesion.SesionEstudioResponseDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.ActividadRepaso;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.Flashcard;
import com.easy4you.model.entity.PreguntaTest;
import com.easy4you.model.entity.ProgresoAsignatura;
import com.easy4you.model.entity.ProgresoTema;
import com.easy4you.model.entity.SesionEstudio;
import com.easy4you.model.entity.Tema;
import com.easy4you.model.entity.Usuario;
import com.easy4you.model.enums.TipoActividadRepaso;
import com.easy4you.repository.ActividadRepasoRepository;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.repository.ProgresoAsignaturaRepository;
import com.easy4you.repository.ProgresoTemaRepository;
import com.easy4you.repository.SesionEstudioRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.service.ProgresoUsuarioService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgresoUsuarioServiceImpl implements ProgresoUsuarioService {

  private static final Logger log = LoggerFactory.getLogger(ProgresoUsuarioServiceImpl.class);

  private static final int MAX_SESIONES_RECIENTES = 10;

  private final SesionEstudioRepository sesionEstudioRepository;
  private final ActividadRepasoRepository actividadRepasoRepository;
  private final ProgresoTemaRepository progresoTemaRepository;
  private final ProgresoAsignaturaRepository progresoAsignaturaRepository;
  private final TemaRepository temaRepository;
  private final PreguntaTestRepository preguntaTestRepository;
  private final FlashcardRepository flashcardRepository;

  @Override
  @Transactional(readOnly = true)
  public ProgresoUsuarioResponseDTO obtenerProgresoUsuario(Long usuarioId) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }

    List<SesionEstudioResponseDTO> sesiones =
        sesionEstudioRepository.findByUsuarioIdOrderByFechaInicioDesc(usuarioId).stream()
            .limit(MAX_SESIONES_RECIENTES)
            .map(this::toSesionResponse)
            .toList();

    long flashcardsRepasadas = actividadRepasoRepository.countByUsuarioIdAndTipo(usuarioId, TipoActividadRepaso.FLASHCARD);
    long testsCompletados = actividadRepasoRepository.countByUsuarioIdAndTipo(usuarioId, TipoActividadRepaso.TEST);

    Map<Long, TemaCounts> countsByTema = buildCountsByTema(usuarioId);

    List<ProgresoTemaResponseDTO> dominioPorTema =
        progresoTemaRepository.findByUsuarioIdOrderByUpdatedAtDesc(usuarioId).stream()
            .map(
                p -> {
                  Long temaId = p.getTema() != null ? p.getTema().getId() : null;
                  TemaCounts counts = temaId != null ? countsByTema.getOrDefault(temaId, new TemaCounts()) : new TemaCounts();
                  return new ProgresoTemaResponseDTO(
                      temaId,
                      safePercent(p.getPorcentaje()),
                      p.getSesionesCompletadas(),
                      p.getMinutosEstudiados(),
                      p.getUltimaSesion(),
                      counts.flashcards,
                      counts.tests);
                })
            .toList();

    return new ProgresoUsuarioResponseDTO(sesiones, flashcardsRepasadas, testsCompletados, dominioPorTema);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgresoTemaResponseDTO obtenerProgresoTema(Long usuarioId, Long temaId) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (temaId == null) {
      throw new BadRequestException("temaId es obligatorio");
    }

    temaRepository
        .findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(temaId, usuarioId)
        .orElseThrow(() -> new NotFoundException("Tema no encontrado: " + temaId));

    ProgresoTema progreso = progresoTemaRepository.findByUsuarioIdAndTemaId(usuarioId, temaId).orElse(null);

    long flashcardsRepasadasTema =
        actividadRepasoRepository.countByUsuarioIdAndTemaIdAndTipo(usuarioId, temaId, TipoActividadRepaso.FLASHCARD);
    long testsCompletadosTema =
        actividadRepasoRepository.countByUsuarioIdAndTemaIdAndTipo(usuarioId, temaId, TipoActividadRepaso.TEST);

    if (progreso == null) {
      return new ProgresoTemaResponseDTO(
          temaId, BigDecimal.ZERO.setScale(2), 0, 0, null, flashcardsRepasadasTema, testsCompletadosTema);
    }

    return new ProgresoTemaResponseDTO(
        temaId,
        safePercent(progreso.getPorcentaje()),
        progreso.getSesionesCompletadas(),
        progreso.getMinutosEstudiados(),
        progreso.getUltimaSesion(),
        flashcardsRepasadasTema,
        testsCompletadosTema);
  }

  @Override
  public void registrarRespuestaTest(Long usuarioId, Long preguntaTestId, boolean correcta) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (preguntaTestId == null) {
      throw new BadRequestException("preguntaTestId es obligatorio");
    }

    PreguntaTest pregunta =
        preguntaTestRepository
            .findByIdAndUsuarioId(preguntaTestId, usuarioId)
            .orElseThrow(() -> new NotFoundException("Pregunta test no encontrada: " + preguntaTestId));

    if (pregunta.getTema() == null || pregunta.getTema().getId() == null) {
      throw new BadRequestException("La pregunta no tiene tema asociado");
    }

    String solucion = correcta ? "Correcta" : "Incorrecta";

    ActividadRepaso act = new ActividadRepaso();
    act.setUsuario(pregunta.getUsuario());
    act.setTema(pregunta.getTema());
    act.setDocumento(pregunta.getDocumento());
    act.setTipo(TipoActividadRepaso.TEST);
    act.setEnunciado(trimToMax(pregunta.getEnunciado(), 8000));
    act.setSolucion(solucion);
    actividadRepasoRepository.save(act);

    updateProgresoFromActividad(pregunta.getUsuario(), pregunta.getTema(), correcta ? 2 : 0);

    log.info(
        "Actividad test registrada: usuarioId={}, preguntaTestId={}, correcta={}", usuarioId, preguntaTestId, correcta);
  }

  @Override
  public void registrarRepasoFlashcard(Long usuarioId, Long flashcardId) {
    if (usuarioId == null) {
      throw new BadRequestException("usuarioId es obligatorio");
    }
    if (flashcardId == null) {
      throw new BadRequestException("flashcardId es obligatorio");
    }

    Flashcard flashcard =
        flashcardRepository
            .findByIdAndUsuarioId(flashcardId, usuarioId)
            .orElseThrow(() -> new NotFoundException("Flashcard no encontrada: " + flashcardId));

    if (flashcard.getTema() == null || flashcard.getTema().getId() == null) {
      throw new BadRequestException("La flashcard no tiene tema asociado");
    }

    ActividadRepaso act = new ActividadRepaso();
    act.setUsuario(flashcard.getUsuario());
    act.setTema(flashcard.getTema());
    act.setDocumento(flashcard.getDocumento());
    act.setTipo(TipoActividadRepaso.FLASHCARD);
    act.setEnunciado(trimToMax(flashcard.getPregunta(), 8000));
    act.setSolucion(trimToMax(flashcard.getRespuesta(), 8000));
    actividadRepasoRepository.save(act);

    updateProgresoFromActividad(flashcard.getUsuario(), flashcard.getTema(), 1);

    log.info("Actividad flashcard registrada: usuarioId={}, flashcardId={}", usuarioId, flashcardId);
  }

  private void updateProgresoFromActividad(Usuario usuario, Tema tema, int porcentajeDelta) {
    if (usuario == null || usuario.getId() == null) {
      return;
    }
    if (tema == null || tema.getId() == null) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();

    ProgresoTema progresoTema =
        progresoTemaRepository
            .findByUsuarioIdAndTemaId(usuario.getId(), tema.getId())
            .orElseGet(
                () -> {
                  ProgresoTema p = new ProgresoTema();
                  p.setUsuario(usuario);
                  p.setTema(tema);
                  return p;
                });

    progresoTema.setSesionesCompletadas(safeInt(progresoTema.getSesionesCompletadas()) + 1);
    progresoTema.setUltimaSesion(now);

    BigDecimal currentTema = safePercent(progresoTema.getPorcentaje());
    BigDecimal updatedTema = clampPercent(currentTema.add(BigDecimal.valueOf(porcentajeDelta)));
    progresoTema.setPorcentaje(updatedTema);
    progresoTemaRepository.save(progresoTema);

    Asignatura asignatura = resolveAsignaturaFromTema(tema);
    if (asignatura == null || asignatura.getId() == null) {
      return;
    }

    ProgresoAsignatura progresoAsignatura =
        progresoAsignaturaRepository
            .findByUsuarioIdAndAsignaturaId(usuario.getId(), asignatura.getId())
            .orElseGet(
                () -> {
                  ProgresoAsignatura p = new ProgresoAsignatura();
                  p.setUsuario(usuario);
                  p.setAsignatura(asignatura);
                  return p;
                });

    progresoAsignatura.setSesionesCompletadas(safeInt(progresoAsignatura.getSesionesCompletadas()) + 1);
    progresoAsignatura.setUltimaSesion(now);

    BigDecimal porcentajeAsignatura = computeAsignaturaPercent(usuario.getId(), asignatura.getId());
    progresoAsignatura.setPorcentaje(porcentajeAsignatura);
    progresoAsignaturaRepository.save(progresoAsignatura);
  }

  private BigDecimal computeAsignaturaPercent(Long usuarioId, Long asignaturaId) {
    List<ProgresoTema> temas = progresoTemaRepository.findByUsuarioIdAndAsignaturaId(usuarioId, asignaturaId);
    if (temas == null || temas.isEmpty()) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    BigDecimal sum = BigDecimal.ZERO;
    int count = 0;
    for (ProgresoTema p : temas) {
      sum = sum.add(safePercent(p != null ? p.getPorcentaje() : null));
      count++;
    }

    if (count <= 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    return clampPercent(sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
  }

  private Asignatura resolveAsignaturaFromTema(Tema tema) {
    if (tema == null
        || tema.getUnidad() == null
        || tema.getUnidad().getResultadoAprendizaje() == null
        || tema.getUnidad().getResultadoAprendizaje().getAsignatura() == null) {
      return null;
    }
    return tema.getUnidad().getResultadoAprendizaje().getAsignatura();
  }

  private SesionEstudioResponseDTO toSesionResponse(SesionEstudio s) {
    List<Long> temaIds =
        s.getTemas() == null
            ? List.of()
            : s.getTemas().stream()
                .filter(Objects::nonNull)
                .map(Tema::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

    return new SesionEstudioResponseDTO(
        s.getId(),
        s.getUsuario() != null ? s.getUsuario().getId() : null,
        s.getTitulo(),
        s.getDescripcion(),
        s.getFechaInicio(),
        s.getFechaFin(),
        s.getEstado(),
        s.getMinutosObjetivo(),
        s.getMinutosReal(),
        temaIds,
        s.getCreatedAt(),
        s.getUpdatedAt());
  }

  private Map<Long, TemaCounts> buildCountsByTema(Long usuarioId) {
    Map<Long, TemaCounts> out = new HashMap<>();
    List<ActividadRepasoRepository.TemaTipoCount> rows = actividadRepasoRepository.countByUsuarioGrouped(usuarioId);
    if (rows == null) {
      return out;
    }

    for (ActividadRepasoRepository.TemaTipoCount row : rows) {
      if (row == null || row.getTemaId() == null || row.getTipo() == null) {
        continue;
      }
      TemaCounts counts = out.computeIfAbsent(row.getTemaId(), id -> new TemaCounts());
      if (row.getTipo() == TipoActividadRepaso.FLASHCARD) {
        counts.flashcards += Math.max(0, row.getTotal());
      } else if (row.getTipo() == TipoActividadRepaso.TEST) {
        counts.tests += Math.max(0, row.getTotal());
      }
    }

    return out;
  }

  private BigDecimal clampPercent(BigDecimal value) {
    BigDecimal v = safePercent(value);
    if (v.compareTo(BigDecimal.ZERO) < 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    BigDecimal max = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
    if (v.compareTo(max) > 0) {
      return max;
    }
    return v;
  }

  private BigDecimal safePercent(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    BigDecimal v = value;
    if (v.scale() != 2) {
      v = v.setScale(2, RoundingMode.HALF_UP);
    }
    return v;
  }

  private int safeInt(Integer v) {
    return v == null ? 0 : v;
  }

  private String trimToMax(String s, int max) {
    if (s == null) {
      return "";
    }
    String t = s.trim();
    if (t.length() <= max) {
      return t;
    }
    return t.substring(0, Math.max(0, max - 1)).trim() + "…";
  }

  private static class TemaCounts {
    private long flashcards = 0;
    private long tests = 0;
  }
}

