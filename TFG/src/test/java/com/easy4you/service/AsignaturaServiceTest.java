package com.easy4you.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.easy4you.exception.NotFoundException;
import com.easy4you.model.entity.Asignatura;
import com.easy4you.repository.AsignaturaRepository;
import com.easy4you.repository.DocumentoRepository;
import com.easy4you.repository.FlashcardRepository;
import com.easy4you.repository.PreguntaTestRepository;
import com.easy4you.repository.TemaRepository;
import com.easy4you.service.impl.AsignaturaServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsignaturaServiceTest {

  @Mock AsignaturaRepository asignaturaRepository;
  @Mock TemaRepository temaRepository;
  @Mock DocumentoRepository documentoRepository;
  @Mock FlashcardRepository flashcardRepository;
  @Mock PreguntaTestRepository preguntaTestRepository;

  @InjectMocks AsignaturaServiceImpl asignaturaService;

  @Test
  void crear_guardaYRetornaAsignatura() {
    Asignatura a = new Asignatura();
    a.setNombre("Matemáticas");
    when(asignaturaRepository.save(a)).thenReturn(a);

    Asignatura resultado = asignaturaService.crear(a);

    assertThat(resultado.getNombre()).isEqualTo("Matemáticas");
    verify(asignaturaRepository).save(a);
  }

  @Test
  void obtenerPorId_lanzaNotFoundExceptionSiNoExiste() {
    when(asignaturaRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> asignaturaService.obtenerPorId(99L));
  }

  @Test
  void eliminar_lanzaNotFoundExceptionSiNoExiste() {
    when(asignaturaRepository.existsById(99L)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> asignaturaService.eliminar(99L));
  }
}

