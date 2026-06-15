package com.easy4you.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.easy4you.dto.unidadtematica.UnidadTematicaRequestDTO;
import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.model.entity.UnidadTematica;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.UnidadTematicaRepository;
import com.easy4you.service.impl.UnidadTematicaServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnidadTematicaServiceTest {

  @Mock UnidadTematicaRepository unidadTematicaRepository;
  @Mock AsignaturaRepository asignaturaRepository;

  @InjectMocks UnidadTematicaServiceImpl unidadTematicaService;

  @Test
  void obtenerPorIdDeUsuario_lanzaNotFoundSiNoExiste() {
    when(unidadTematicaRepository.findByIdAndAsignaturaUsuarioId(9L, 1L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> unidadTematicaService.obtenerPorIdDeUsuario(1L, 9L));
  }

  @Test
  void crearDeUsuario_creaUnidadYGuarda() {
    Asignatura a = new Asignatura();
    a.setId(3L);
    when(asignaturaRepository.findByIdAndUsuarioId(3L, 1L)).thenReturn(Optional.of(a));
    when(unidadTematicaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UnidadTematicaRequestDTO req = new UnidadTematicaRequestDTO(3L, "Unidad 1", 1, null);
    UnidadTematica out = unidadTematicaService.crearDeUsuario(1L, req);

    assertThat(out.getAsignatura()).isEqualTo(a);
    assertThat(out.getTitulo()).isEqualTo("Unidad 1");
    assertThat(out.getOrden()).isEqualTo(1);
    verify(unidadTematicaRepository).save(any(UnidadTematica.class));
  }
}

