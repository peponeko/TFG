package com.easy4you.service;

import com.easy4you.dto.documento.DocumentoBusquedaResultadoDTO;
import com.easy4you.dto.documento.DocumentoChunksPageResponseDTO;
import com.easy4you.dto.documento.DocumentoDetalleResponseDTO;
import com.easy4you.dto.documento.DocumentoEstadoResponseDTO;
import com.easy4you.dto.documento.DocumentoResponseDTO;
import com.easy4you.model.entity.Documento;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface DocumentoService {
  List<Documento> listar();

  List<Documento> listarPorUsuarioId(Long usuarioId);

  List<Documento> listarPorTemaId(Long temaId);

  List<Documento> listarPorAsignaturaIdDeUsuario(Long usuarioId, Long asignaturaId);

  /**
   * Filtra como GET /api/documentos y devuelve DTOs ligeros. Todo el trabajo ocurre dentro de esta transacción
   * del servicio (no depende de {@code @Transactional} en el controlador).
   */
  List<DocumentoResponseDTO> listarItemsDto(Long usuarioActualId, Long temaId, Long asignaturaId);

  DocumentoResponseDTO obtenerResponsePorIdDeUsuario(Long usuarioId, Long documentoId);

  Documento crear(Documento documento);

  Documento obtenerPorId(Long id);

  Documento actualizar(Long id, Documento documento);

  void eliminar(Long id);

  Documento obtenerPorIdDeUsuario(Long usuarioId, Long documentoId);

  DocumentoEstadoResponseDTO estado(Long usuarioId, Long documentoId);

  DocumentoChunksPageResponseDTO chunks(Long usuarioId, Long documentoId, Pageable pageable);

  List<DocumentoBusquedaResultadoDTO> buscar(Long usuarioId, String q, Long asignaturaId, Long temaId);

  DocumentoDetalleResponseDTO detalle(Long usuarioId, Long documentoId, Pageable pageable);
}
