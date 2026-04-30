package com.easy4you.service.impl;

import com.easy4you.dto.tema.TemaRapidoRequestDTO;
import com.easy4you.dto.tema.TemaRequestDTO;
import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.ResultadoAprendizaje;
import com.easy4you.model.entity.Tema;
import com.easy4you.model.entity.Unidad;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.ResultadoAprendizajeRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.repository.UnidadRepository;
import com.easy4you.service.TemaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TemaServiceImpl implements TemaService {

  private final TemaRepository temaRepository;
  private final AsignaturaRepository asignaturaRepository;
  private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
  private final UnidadRepository unidadRepository;

  @Override
  @Transactional(readOnly = true)
  public List<Tema> listar() {
    return temaRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Tema> listarPorUnidadId(Long unidadId) {
    return temaRepository.findByUnidadIdOrderByOrdenAsc(unidadId);
  }

  @Override
  public Tema crear(Tema tema) {
    return temaRepository.save(tema);
  }

  @Override
  @Transactional(readOnly = true)
  public Tema obtenerPorId(Long id) {
    return temaRepository.findById(id).orElseThrow(() -> new NotFoundException("Tema no encontrado: " + id));
  }

  @Override
  public Tema actualizar(Long id, Tema datos) {
    Tema existente = obtenerPorId(id);
    if (datos.getUnidad() != null) {
      existente.setUnidad(datos.getUnidad());
    }
    existente.setTitulo(datos.getTitulo());
    existente.setDescripcion(datos.getDescripcion());
    existente.setOrden(datos.getOrden());
    existente.setPalabrasClave(datos.getPalabrasClave());
    return temaRepository.save(existente);
  }

  @Override
  public void eliminar(Long id) {
    if (!temaRepository.existsById(id)) {
      throw new NotFoundException("Tema no encontrado: " + id);
    }
    temaRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Tema> listarPorAsignaturaIdDeUsuario(Long usuarioId, Long asignaturaId) {
    if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioId).isEmpty()) {
      throw new NotFoundException("Asignatura no encontrada: " + asignaturaId);
    }
    return temaRepository.findByUnidadResultadoAprendizajeAsignaturaIdOrderByOrdenAsc(asignaturaId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Tema> listarPorUnidadIdDeUsuario(Long usuarioId, Long unidadId) {
    if (!unidadRepository.existsByIdAndResultadoAprendizajeAsignaturaUsuarioId(unidadId, usuarioId)) {
      throw new NotFoundException("Unidad no encontrada: " + unidadId);
    }
    return listarPorUnidadId(unidadId);
  }

  @Override
  @Transactional(readOnly = true)
  public Tema obtenerPorIdDeUsuario(Long usuarioId, Long temaId) {
    return temaRepository
        .findByIdAndUnidadResultadoAprendizajeAsignaturaUsuarioId(temaId, usuarioId)
        .orElseThrow(() -> new NotFoundException("Tema no encontrado: " + temaId));
  }

  @Override
  public Tema crearDeUsuario(Long usuarioId, TemaRequestDTO request) {
    Unidad unidad = resolveUnidadParaCrearTema(usuarioId, request);

    Tema tema = new Tema();
    tema.setUnidad(unidad);
    tema.setTitulo(request.getTitulo());
    tema.setDescripcion(request.getDescripcion());
    tema.setOrden(request.getOrden() != null ? request.getOrden() : 0);
    tema.setPalabrasClave(request.getPalabrasClave());

    return crear(tema);
  }

  @Override
  public Tema crearRapidoDeUsuario(Long usuarioId, TemaRapidoRequestDTO request) {
    if (request.getAsignaturaId() == null) {
      throw new BadRequestException("asignaturaId es obligatorio");
    }

    Integer trimestre = request.getTrimestre();
    if (trimestre != null && !(trimestre == 0 || trimestre == 1 || trimestre == 2 || trimestre == 3)) {
      throw new BadRequestException("trimestre inválido. Usa 1, 2, 3 o 0/null");
    }

    Asignatura asignatura =
        asignaturaRepository
            .findByIdAndUsuarioId(request.getAsignaturaId(), usuarioId)
            .orElseThrow(
                () -> new NotFoundException("Asignatura no encontrada: " + request.getAsignaturaId()));

    ResultadoAprendizaje ra = resolveResultadoAprendizajeTrimestre(asignatura, trimestre);
    Unidad unidad = resolveUnidadPrincipal(ra);

    Tema tema = new Tema();
    tema.setUnidad(unidad);
    tema.setTitulo(request.getTitulo());
    tema.setDescripcion(request.getDescripcion());
    tema.setOrden(0);
    tema.setPalabrasClave(request.getPalabrasClave());

    return crear(tema);
  }

  @Override
  public Tema actualizarDeUsuario(Long usuarioId, Long temaId, TemaRequestDTO request) {
    Tema existente = obtenerPorIdDeUsuario(usuarioId, temaId);

    Tema datos = new Tema();
    datos.setUnidad(existente.getUnidad());
    datos.setTitulo(request.getTitulo());
    datos.setDescripcion(request.getDescripcion());
    datos.setOrden(request.getOrden() != null ? request.getOrden() : existente.getOrden());
    datos.setPalabrasClave(request.getPalabrasClave());

    if (request.getUnidadId() != null
        && (existente.getUnidad() == null || !request.getUnidadId().equals(existente.getUnidad().getId()))) {
      if (!unidadRepository.existsByIdAndResultadoAprendizajeAsignaturaUsuarioId(request.getUnidadId(), usuarioId)) {
        throw new NotFoundException("Unidad no encontrada: " + request.getUnidadId());
      }
      Unidad unidad = unidadRepository.findById(request.getUnidadId())
          .orElseThrow(() -> new NotFoundException("Unidad no encontrada: " + request.getUnidadId()));
      datos.setUnidad(unidad);
    }

    return actualizar(temaId, datos);
  }

  @Override
  public void eliminarDeUsuario(Long usuarioId, Long temaId) {
    obtenerPorIdDeUsuario(usuarioId, temaId);
    eliminar(temaId);
  }

  private Unidad resolveUnidadParaCrearTema(Long usuarioId, TemaRequestDTO request) {
    if (request == null) {
      throw new BadRequestException("Body es obligatorio");
    }

    if (request.getUnidadId() != null) {
      if (!unidadRepository.existsByIdAndResultadoAprendizajeAsignaturaUsuarioId(request.getUnidadId(), usuarioId)) {
        throw new NotFoundException("Unidad no encontrada: " + request.getUnidadId());
      }
      return unidadRepository
          .findById(request.getUnidadId())
          .orElseThrow(() -> new NotFoundException("Unidad no encontrada: " + request.getUnidadId()));
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

  private ResultadoAprendizaje resolveResultadoAprendizajeTrimestre(Asignatura asignatura, Integer trimestre) {
    if (asignatura == null || asignatura.getId() == null) {
      throw new BadRequestException("Asignatura inválida");
    }

    if (trimestre == null || trimestre == 0) {
      return resultadoAprendizajeRepository
          .findTopByAsignaturaIdAndCodigoIgnoreCaseOrderByIdAsc(asignatura.getId(), "GEN")
          .orElseGet(
              () -> {
                ResultadoAprendizaje nuevo = new ResultadoAprendizaje();
                nuevo.setAsignatura(asignatura);
                nuevo.setCodigo("GEN");
                nuevo.setDescripcion("General");
                nuevo.setOrden(0);
                return resultadoAprendizajeRepository.save(nuevo);
              });
    }

    String codigo = "T" + trimestre;
    return resultadoAprendizajeRepository
        .findTopByAsignaturaIdAndCodigoIgnoreCaseOrderByIdAsc(asignatura.getId(), codigo)
        .orElseGet(
            () -> {
              ResultadoAprendizaje nuevo = new ResultadoAprendizaje();
              nuevo.setAsignatura(asignatura);
              nuevo.setCodigo(codigo);
              nuevo.setDescripcion("Trimestre " + trimestre);
              nuevo.setOrden(trimestre);
              return resultadoAprendizajeRepository.save(nuevo);
            });
  }

  private Unidad resolveUnidadPrincipal(ResultadoAprendizaje ra) {
    if (ra == null || ra.getId() == null) {
      throw new BadRequestException("Resultado de aprendizaje inválido");
    }

    return unidadRepository.findByResultadoAprendizajeIdOrderByOrdenAsc(ra.getId()).stream()
        .filter(u -> u.getTitulo() != null && u.getTitulo().trim().equalsIgnoreCase("Unidad Principal"))
        .findFirst()
        .orElseGet(
            () -> {
              Unidad unidad = new Unidad();
              unidad.setResultadoAprendizaje(ra);
              unidad.setTitulo("Unidad Principal");
              unidad.setDescripcion(null);
              unidad.setOrden(1);
              return unidadRepository.save(unidad);
            });
  }
}
