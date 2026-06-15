package com.easy4you.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.EventoCalendario;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.EventoCalendarioRepository;
import com.easy4you.repository.UsuarioRepository;
import com.easy4you.service.impl.EventoCalendarioServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventoCalendarioServiceTest {

  @Mock EventoCalendarioRepository eventoCalendarioRepository;
  @Mock AsignaturaRepository asignaturaRepository;
  @Mock UsuarioRepository usuarioRepository;

  @InjectMocks EventoCalendarioServiceImpl eventoCalendarioService;

  @Test
  void obtenerPorIdDeUsuario_lanzaNotFoundSiNoExiste() {
    when(eventoCalendarioRepository.findByIdAndUsuarioId(5L, 1L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> eventoCalendarioService.obtenerPorIdDeUsuario(1L, 5L));
  }

  @Test
  void toggleCompletado_cambiaEstadoYGuarda() {
    EventoCalendario e = new EventoCalendario();
    e.setId(5L);
    e.setCompletado(false);
    when(eventoCalendarioRepository.findByIdAndUsuarioId(5L, 1L)).thenReturn(Optional.of(e));
    when(eventoCalendarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    EventoCalendario out = eventoCalendarioService.toggleCompletado(1L, 5L);

    assertThat(out.isCompletado()).isTrue();
    verify(eventoCalendarioRepository).save(e);
  }
}

