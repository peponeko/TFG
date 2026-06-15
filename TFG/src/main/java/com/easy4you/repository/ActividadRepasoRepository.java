package com.easy4you.repository;

import com.easy4you.model.entity.ActividadRepaso;
import com.easy4you.model.enums.TipoActividadRepaso;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ActividadRepasoRepository extends JpaRepository<ActividadRepaso, Long> {
  List<ActividadRepaso> findByTemaId(Long temaId);

  long countByUsuarioIdAndTipo(Long usuarioId, TipoActividadRepaso tipo);

  long countByUsuarioIdAndTemaIdAndTipo(Long usuarioId, Long temaId, TipoActividadRepaso tipo);

  @Query(
      """
      select a.tema.id as temaId, a.tipo as tipo, count(a) as total
      from ActividadRepaso a
      where a.usuario.id = :usuarioId
      group by a.tema.id, a.tipo
      """)
  List<TemaTipoCount> countByUsuarioGrouped(@Param("usuarioId") Long usuarioId);

  interface TemaTipoCount {
    Long getTemaId();

    TipoActividadRepaso getTipo();

    long getTotal();
  }
}
