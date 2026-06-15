package com.easy4you.service.impl;

import com.easy4you.exception.BadRequestException;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.NotebookCompartido;
import com.easy4you.model.entity.Usuario;
import com.easy4you.model.enums.RolNotebookCompartido;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.NotebookCompartidoRepository;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.service.NotebookCompartidoService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotebookCompartidoServiceImpl implements NotebookCompartidoService {

  private static final Logger log = LoggerFactory.getLogger(NotebookCompartidoServiceImpl.class);

  private final NotebookCompartidoRepository notebookCompartidoRepository;
  private final AsignaturaRepository asignaturaRepository;
  private final UsuarioRepository usuarioRepository;

  @Override
  public NotebookCompartido compartir(
      Long propietarioId, Long asignaturaId, Long usuarioInvitadoId, RolNotebookCompartido rol) {
    if (propietarioId == null) {
      throw new BadRequestException("propietarioId es obligatorio");
    }
    if (asignaturaId == null) {
      throw new BadRequestException("asignaturaId es obligatorio");
    }
    if (usuarioInvitadoId == null) {
      throw new BadRequestException("usuarioInvitadoId es obligatorio");
    }
    if (propietarioId.equals(usuarioInvitadoId)) {
      throw new BadRequestException("No puedes compartir contigo mismo");
    }

    Asignatura asignatura =
        asignaturaRepository
            .findByIdAndUsuarioId(asignaturaId, propietarioId)
            .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + asignaturaId));

    Usuario invitado =
        usuarioRepository
            .findById(usuarioInvitadoId)
            .orElseThrow(() -> new NotFoundException("Usuario invitado no encontrado: " + usuarioInvitadoId));

    RolNotebookCompartido resolvedRole = rol != null ? rol : RolNotebookCompartido.VIEWER;

    NotebookCompartido compartido =
        notebookCompartidoRepository
            .findByAsignaturaIdAndUsuarioInvitadoId(asignaturaId, usuarioInvitadoId)
            .orElseGet(NotebookCompartido::new);

    if (compartido.getId() != null
        && compartido.getPropietario() != null
        && compartido.getPropietario().getId() != null
        && !propietarioId.equals(compartido.getPropietario().getId())) {
      throw new BadRequestException("No puedes modificar un compartido que no es tuyo");
    }

    compartido.setAsignatura(asignatura);
    compartido.setPropietario(asignatura.getUsuario());
    compartido.setUsuarioInvitado(invitado);
    compartido.setRol(resolvedRole);

    NotebookCompartido saved = notebookCompartidoRepository.save(compartido);
    log.info(
        "Notebook compartido: asignaturaId={}, propietarioId={}, invitadoId={}, rol={}",
        asignaturaId,
        propietarioId,
        usuarioInvitadoId,
        resolvedRole);
    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public List<NotebookCompartido> listarCompartidosConmigo(Long usuarioInvitadoId) {
    if (usuarioInvitadoId == null) {
      throw new BadRequestException("usuarioInvitadoId es obligatorio");
    }
    return notebookCompartidoRepository.findByUsuarioInvitadoId(usuarioInvitadoId);
  }

  @Override
  public void revocar(Long propietarioId, Long asignaturaId, Long usuarioInvitadoId) {
    if (propietarioId == null) {
      throw new BadRequestException("propietarioId es obligatorio");
    }
    if (asignaturaId == null) {
      throw new BadRequestException("asignaturaId es obligatorio");
    }
    if (usuarioInvitadoId == null) {
      throw new BadRequestException("usuarioInvitadoId es obligatorio");
    }

    asignaturaRepository
        .findByIdAndUsuarioId(asignaturaId, propietarioId)
        .orElseThrow(() -> new NotFoundException("Asignatura no encontrada: " + asignaturaId));

    NotebookCompartido compartido =
        notebookCompartidoRepository
            .findByAsignaturaIdAndUsuarioInvitadoId(asignaturaId, usuarioInvitadoId)
            .orElseThrow(() -> new NotFoundException("Compartición no encontrada"));

    if (compartido.getPropietario() == null
        || compartido.getPropietario().getId() == null
        || !propietarioId.equals(compartido.getPropietario().getId())) {
      throw new NotFoundException("Compartición no encontrada");
    }

    notebookCompartidoRepository.delete(compartido);
    log.info(
        "Notebook revocado: asignaturaId={}, propietarioId={}, invitadoId={}",
        asignaturaId,
        propietarioId,
        usuarioInvitadoId);
  }

  @Override
  public boolean tieneAcceso(Long usuarioId, Long asignaturaId) {
    if (usuarioId == null || asignaturaId == null) {
      return false;
    }

    // Verificar si es el propietario
    if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioId).isPresent()) {
      return true;
    }

    // Verificar si es invitado
    return notebookCompartidoRepository
        .findByAsignaturaIdAndUsuarioInvitadoId(asignaturaId, usuarioId)
        .isPresent();
  }

  @Override
  public Optional<RolNotebookCompartido> obtenerRol(Long usuarioId, Long asignaturaId) {
    if (usuarioId == null || asignaturaId == null) {
      return Optional.empty();
    }

    // Si es el propietario, tiene rol EDITOR implícito
    if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioId).isPresent()) {
      return Optional.of(RolNotebookCompartido.EDITOR);
    }

    // Verificar si es invitado
    return notebookCompartidoRepository
        .findByAsignaturaIdAndUsuarioInvitadoId(asignaturaId, usuarioId)
        .map(NotebookCompartido::getRol);
  }

  @Override
  public boolean puedeEditar(Long usuarioId, Long asignaturaId) {
    if (usuarioId == null || asignaturaId == null) {
      return false;
    }

    // Si es el propietario, puede editar
    if (asignaturaRepository.findByIdAndUsuarioId(asignaturaId, usuarioId).isPresent()) {
      return true;
    }

    // Verificar si es invitado con rol EDITOR
    return notebookCompartidoRepository
        .findByAsignaturaIdAndUsuarioInvitadoId(asignaturaId, usuarioId)
        .map(nc -> nc.getRol() == RolNotebookCompartido.EDITOR)
        .orElse(false);
  }

  @Override
  public Long obtenerPropietarioId(Long asignaturaId) {
    if (asignaturaId == null) {
      return null;
    }

    return asignaturaRepository
        .findById(asignaturaId)
        .map(asignatura -> asignatura.getUsuario() != null ? asignatura.getUsuario().getId() : null)
        .orElse(null);
  }
}

