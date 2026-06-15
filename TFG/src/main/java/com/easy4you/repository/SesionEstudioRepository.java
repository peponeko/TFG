package com.easy4you.repository;

import com.easy4you.model.entity.SesionEstudio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SesionEstudioRepository extends JpaRepository<SesionEstudio, Long> {
  List<SesionEstudio> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);
}

