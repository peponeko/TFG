package com.easy4you.repository;

import com.easy4you.model.entity.NotebookCompartido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotebookCompartidoRepository extends JpaRepository<NotebookCompartido, Long> {
  List<NotebookCompartido> findByUsuarioInvitadoId(Long usuarioInvitadoId);

  Optional<NotebookCompartido> findByAsignaturaIdAndUsuarioInvitadoId(Long asignaturaId, Long usuarioInvitadoId);
}

