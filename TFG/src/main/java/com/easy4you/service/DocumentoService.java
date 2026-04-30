package com.easy4you.service;

import com.easy4you.dto.documento.DocumentoBusquedaResultadoDTO;
import com.easy4you.dto.documento.DocumentoChunksPageResponseDTO;
import com.easy4you.dto.documento.DocumentoDetalleResponseDTO;
import com.easy4you.dto.documento.DocumentoEstadoResponseDTO;
import com.easy4you.model.entity.Documento;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface DocumentoService {
  List<Documento> listar();

  List<Documento> listarPorUsuarioId(Long usuarioId);

  List<Documento> listarPorTemaId(Long temaId);

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
